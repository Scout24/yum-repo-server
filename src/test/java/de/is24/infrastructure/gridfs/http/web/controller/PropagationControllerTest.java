package de.is24.infrastructure.gridfs.http.web.controller;

import de.is24.infrastructure.gridfs.http.exception.GridFSFileNotFoundException;
import org.junit.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class PropagationControllerTest extends AbstractControllerTest {

  public static final String DEST_REPO = "dest-repo";

  @Test
  public void throwExceptionForFileNotFound() throws Exception {
    doThrow(GridFSFileNotFoundException.class).when(gridFsService).propagateRpm(anyString(), anyString());

    mockMvc.perform(postPorpagationWithSourceFile("not/existing/file")).andExpect(status().isNotFound());
  }

  private MockHttpServletRequestBuilder postPorpagationWithSourceFile(String sourceFile) {
    return post("/propagation").param("source", sourceFile).param("destination", DEST_REPO);
  }

  @Test
  public void moveFileToDestRepo() throws Exception {

    final String sourceFile = "my/existing/file";
    mockMvc.perform(postPorpagationWithSourceFile(sourceFile)).andExpect(status().isCreated());

    verify(gridFsService).propagateRpm(sourceFile, DEST_REPO);
  }

}
