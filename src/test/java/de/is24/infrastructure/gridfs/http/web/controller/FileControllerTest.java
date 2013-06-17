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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;

import static com.mongodb.gridfs.GridFSTestUtil.gridFSDBFile;
import static org.apache.commons.lang.RandomStringUtils.random;
import static org.apache.commons.lang.StringUtils.repeat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.valueOf;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;



public class FileControllerTest extends AbstractControllerTest {
  public static final MediaType APPLICATION_X_RPM = valueOf("application/x-rpm");
  public static final String REPO = "files";
  public static final String ARCH = "test";
  public static final String FILENAME = "file";

  public static final String RPM_URL = "/repo/" + REPO + "/" + ARCH + "/" + FILENAME + ".rpm";
  private static final MockHttpServletRequestBuilder DELETE_REQUEST = MockMvcRequestBuilders.delete(RPM_URL);


  public static final String CONTENT_WITH_200_CHARS = random(200, "a");

  

  @SuppressWarnings("unchecked")
  @Test
  public void get404ResponseWhenFileIsNotFound() throws Exception {
    when(gridFs.getResource(anyString())).thenThrow(GridFSFileNotFoundException.class);

    performRpmGet().andExpect(status().isNotFound());
  }
  
  @Test
  public void deliverFileWhenFileIsFoundInDB() throws Exception {
    givenGridFSDBFile();

    performRpmGet()
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_X_RPM))
        .andExpect(content().string(CONTENT_WITH_200_CHARS))
        .andExpect(contentLengthIs(200));
  }

  @Test
  public void deliverFirstBytesWhenByteRangeRequestWithEnd() throws Exception {
    givenGridFSDBFile();
    
    performRpmGetWithRange("bytes=0-100")
        .andExpect(status().isPartialContent())
        .andExpect(header().string("Accept-Ranges", "bytes"))
        .andExpect(header().string("Content-Range", "bytes 0-100/200"))
        .andExpect(contentLengthIs(101))
        .andExpect(content().contentType(APPLICATION_X_RPM))
        .andExpect(content().string(repeat("a", 101)));
  }

  @Test
  public void deliver416WhenByteRangeInvalidChars() throws Exception {
    givenGridFSDBFile();
    performRpmGetWithRange("bytes=0-abc").andExpect(badRangeStatus());
  }


  @Test
  public void deliver416WhenByteRangeInvalidNumFormat() throws Exception {
    givenGridFSDBFile();
    performRpmGetWithRange("bytes=0-001").andExpect(badRangeStatus());
  }

  @Test
  public void deliver416WhenByteRangeEndCouldNotBeParsedAsLong() throws Exception {
    givenGridFSDBFile();
    performRpmGetWithRange("bytes=0-12345678912345679812").andExpect(badRangeStatus());
  }

  @Test
  public void deliver416WhenByteRangeStartCouldNotBeParsedAsLong() throws Exception {
    givenGridFSDBFile();
    performRpmGetWithRange("bytes=12345678912345679812-").andExpect(badRangeStatus());
  }

  @Test
  public void deliver416WhenByteRangeStartIsBeforeRangeEnd() throws Exception {
    givenGridFSDBFile();
    performRpmGetWithRange("bytes=123-100").andExpect(badRangeStatus());
  }

  @Test
  public void deliver204IfRpmDoesNotExists() throws Exception {
    doThrow(GridFSFileNotFoundException.class).when(gridFs).delete(anyString());

    mockMvc.perform(DELETE_REQUEST).andExpect(status().isNoContent());
  }

  @Test
  public void deliver204IfRpmIsRemoved() throws Exception {
    givenGridFSDBFile();
    mockMvc.perform(DELETE_REQUEST).andExpect(status().isNoContent());
  }

  private void givenGridFSDBFile() throws IOException {
    GridFSDBFile gridFSDBFile = gridFSDBFile(CONTENT_WITH_200_CHARS);

    when(gridFs.getFileByPath(anyString())).thenReturn(gridFSDBFile);
    when(gridFs.getResource(anyString())).thenCallRealMethod();
    when(gridFs.getResource(anyString(), anyLong())).thenCallRealMethod();
    when(gridFs.getResource(anyString(), anyLong(), anyLong())).thenCallRealMethod();
  }

  private ResultActions performRpmGet() throws Exception {
    return mockMvc.perform(MockMvcRequestBuilders.get(RPM_URL));
  }

  private ResultMatcher contentLengthIs(final int length) {
    return new ResultMatcher() {
      @Override
      public void match(MvcResult result) throws Exception {
        assertEquals("Response Content-Length", length, result.getResponse().getContentLength());
      }
    };
  }

  private ResultMatcher badRangeStatus() {
    return status().is(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value());
  }

  private ResultActions performRpmGetWithRange(final String rangeHeader) throws Exception {
    return mockMvc.perform(MockMvcRequestBuilders.get(RPM_URL).header("Range", rangeHeader));
  }

  private String toString(ResponseEntity<InputStreamResource> response) throws IOException {
    return IOUtils.toString(response.getBody().getInputStream());
  }


}
