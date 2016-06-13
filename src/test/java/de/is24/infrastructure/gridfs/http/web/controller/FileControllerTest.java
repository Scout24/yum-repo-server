package de.is24.infrastructure.gridfs.http.web.controller;

import de.is24.infrastructure.gridfs.http.exception.GridFSFileNotFoundException;
import de.is24.infrastructure.gridfs.http.storage.FileDescriptor;
import de.is24.infrastructure.gridfs.http.storage.FileStorageItem;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.apache.commons.lang.RandomStringUtils.random;
import static org.apache.commons.lang.StringUtils.repeat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.valueOf;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


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
    when(fileStorageService.getResource(any(FileDescriptor.class))).thenThrow(GridFSFileNotFoundException.class);

    performRpmGet().andExpect(status().isNotFound());
  }

  @Test
  public void deliverFileWhenFileIsFoundInDB() throws Exception {
    givenGridFSDBFile();

    performRpmGet().andExpect(status().isOk())
    .andExpect(content().contentType(APPLICATION_X_RPM))
    .andExpect(content().string(CONTENT_WITH_200_CHARS))
    .andExpect(contentLengthIs(200));
  }

  @Test
  public void deliverFirstBytesWhenByteRangeRequestWithEnd() throws Exception {
    givenGridFSDBFile();

    performRpmGetWithRange("bytes=0-100").andExpect(status().isPartialContent())
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
    doThrow(GridFSFileNotFoundException.class).when(storageService).delete(any(FileDescriptor.class));

    mockMvc.perform(DELETE_REQUEST).andExpect(status().isNoContent());
  }

  @Test
  public void deliver204IfRpmIsRemoved() throws Exception {
    givenGridFSDBFile();
    mockMvc.perform(DELETE_REQUEST).andExpect(status().isNoContent());
  }

  private void givenGridFSDBFile() throws IOException {
    FileStorageItem fileStorageItem = storageItem(CONTENT_WITH_200_CHARS);

    when(fileStorageService.getFileBy(any(FileDescriptor.class))).thenReturn(fileStorageItem);
    when(fileStorageService.getResource(any(FileDescriptor.class))).thenCallRealMethod();
    when(fileStorageService.getResource(any(FileDescriptor.class), anyLong())).thenCallRealMethod();
    when(fileStorageService.getResource(any(FileDescriptor.class), anyLong(), anyLong())).thenCallRealMethod();
  }

  private ResultActions performRpmGet() throws Exception {
    return performSimpleGet(RPM_URL);
  }

  private ResultMatcher contentLengthIs(final int length) {
    return result -> assertEquals("Response Content-Length", length, result.getResponse().getContentLength());
  }


  private ResultActions performRpmGetWithRange(final String rangeHeader) throws Exception {
    return mockMvc.perform(MockMvcRequestBuilders.get(RPM_URL).header("Range", rangeHeader));
  }

  private String toString(ResponseEntity<InputStreamResource> response) throws IOException {
    return IOUtils.toString(response.getBody().getInputStream());
  }

  private FileStorageItem storageItem(String content) {
    FileStorageItem storageItem = mock(FileStorageItem.class);
    when(storageItem.getInputStream()).thenReturn(new ByteArrayInputStream(content.getBytes()));
    when(storageItem.getSize()).thenReturn((long) content.length());
    when(storageItem.getContentType()).thenReturn("application/x-rpm");
    when(storageItem.getFilename()).thenReturn("files/test/file.rpm");
    return storageItem;
  }
}
