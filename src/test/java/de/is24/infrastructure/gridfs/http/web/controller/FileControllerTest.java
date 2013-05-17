package de.is24.infrastructure.gridfs.http.web.controller;

import com.mongodb.gridfs.GridFSDBFile;
import de.is24.infrastructure.gridfs.http.exception.BadRangeRequestException;
import de.is24.infrastructure.gridfs.http.exception.GridFSFileNotFoundException;
import de.is24.infrastructure.gridfs.http.gridfs.GridFsService;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import java.io.IOException;
import static com.mongodb.gridfs.GridFSTestUtil.gridFSDBFile;
import static org.apache.commons.lang.RandomStringUtils.random;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.PARTIAL_CONTENT;
import static org.springframework.http.MediaType.valueOf;


@RunWith(MockitoJUnitRunner.class)
public class FileControllerTest {
  public static final MediaType APPLICATION_X_RPM = valueOf("application/x-rpm");
  public static final String REPO = "files";
  public static final String ARCH = "test";
  public static final String FILENAME = "file";
  @Mock
  private GridFsService gridFs;
  private MockHttpServletResponse httpServletResponse;

  private FileController fileController;
  public static final String CONTENT_WITH_200_CHARS = random(200, "a");

  @Before
  public void setUp() throws Exception {
    fileController = new FileController(gridFs);
    httpServletResponse = new MockHttpServletResponse();
  }

  @Test(expected = GridFSFileNotFoundException.class)
  public void get404ResponseWhenFileIsNotFound() throws Exception {
    when(gridFs.getResource(anyString())).thenThrow(GridFSFileNotFoundException.class);

    fileController.deliverFile(REPO, ARCH, FILENAME);
  }

  @Test
  public void deliverFileWhenFileIsFoundInDB() throws Exception {
    givenGridFSDBFile();

    ResponseEntity<InputStreamResource> response = fileController.deliverFile(REPO, ARCH, FILENAME);

    assertThat(response.getStatusCode(), is(OK));
    assertThat(response.getHeaders().getContentType(), is(APPLICATION_X_RPM));
    assertThat(response.getHeaders().getContentLength(), is(200L));
    assertThat(toString(response), is(CONTENT_WITH_200_CHARS));
  }

  @Test
  public void deliverFirstBytesWhenByteRangeRequestWithEnd() throws Exception {
    givenGridFSDBFile();

    ResponseEntity<InputStreamResource> response = fileController.deliverRangeOfFile(REPO, ARCH, FILENAME,
      "bytes=0-100");

    assertThat(response.getStatusCode(), is(PARTIAL_CONTENT));
    assertThat(response.getHeaders().get("Accept-Ranges").get(0), is("bytes"));
    assertThat(response.getHeaders().get("Content-Range").get(0), is("bytes 0-100/200"));
    assertThat(response.getHeaders().getContentLength(), is(101L));
    assertThat(response.getHeaders().getContentType(), is(APPLICATION_X_RPM));
    assertThat(toString(response), is(random(101, "a")));
  }

  @Test(expected = BadRangeRequestException.class)
  public void deliver416WhenByteRangeInvalidChars() throws Exception {
    givenGridFSDBFile();
    fileController.deliverRangeOfFile(REPO, ARCH, FILENAME, "bytes=0-abc");
  }

  @Test(expected = BadRangeRequestException.class)
  public void deliver416WhenByteRangeInvalidNumFormat() throws Exception {
    givenGridFSDBFile();
    fileController.deliverRangeOfFile(REPO, ARCH, FILENAME, "bytes=0-001");
  }

  @Test(expected = BadRangeRequestException.class)
  public void deliver416WhenByteRangeEndCouldNotBeParsedAsLong() throws Exception {
    givenGridFSDBFile();
    fileController.deliverRangeOfFile(REPO, ARCH, FILENAME, "bytes=0-12345678912345679812");
  }

  @Test(expected = BadRangeRequestException.class)
  public void deliver416WhenByteRangeStartCouldNotBeParsedAsLong() throws Exception {
    givenGridFSDBFile();
    fileController.deliverRangeOfFile(REPO, ARCH, FILENAME, "bytes=12345678912345679812-");
  }

  @Test(expected = BadRangeRequestException.class)
  public void deliver416WhenByteRangeStartIsBeforeRangeEnd() throws Exception {
    givenGridFSDBFile();
    fileController.deliverRangeOfFile(REPO, ARCH, FILENAME, "bytes=123-100");
  }

  @Test(expected = GridFSFileNotFoundException.class)
  public void deliver404IfRpmDoesNotExists() throws Exception {
    doThrow(GridFSFileNotFoundException.class).when(gridFs).delete(anyString());

    fileController.deleteFile(REPO, ARCH, FILENAME, null);
  }

  @Test
  public void deliver204IfRpmIsRemoved() throws Exception {
    givenGridFSDBFile();

    fileController.deleteFile(REPO, ARCH, FILENAME, httpServletResponse);

    assertThat(httpServletResponse.getStatus(), is(204));
  }

  private void givenGridFSDBFile() throws IOException {
    GridFSDBFile gridFSDBFile = gridFSDBFile(CONTENT_WITH_200_CHARS);

    when(gridFs.getFileByPath(anyString())).thenReturn(gridFSDBFile);
    when(gridFs.getResource(anyString())).thenCallRealMethod();
    when(gridFs.getResource(anyString(), anyLong())).thenCallRealMethod();
    when(gridFs.getResource(anyString(), anyLong(), anyLong())).thenCallRealMethod();
  }

  private String toString(ResponseEntity<InputStreamResource> response) throws IOException {
    return IOUtils.toString(response.getBody().getInputStream());
  }


}
