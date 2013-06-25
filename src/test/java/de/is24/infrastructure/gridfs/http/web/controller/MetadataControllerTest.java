package de.is24.infrastructure.gridfs.http.web.controller;

import de.is24.infrastructure.gridfs.http.exception.BadRequestException;
import de.is24.infrastructure.gridfs.http.metadata.MetadataService;
import de.is24.infrastructure.gridfs.http.repos.RepoService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class MetadataControllerTest extends AbstractControllerTest{
  private static final String ANY_REPONAME = "any-reponame";

  @Test
  public void throwsBadRequestExceptionIfRepoIsScheduled() throws Exception {
    when(repoService.isRepoScheduled(ANY_REPONAME)).thenReturn(true);

    performGenerateMetaDataPost()
        .andExpect(status().isBadRequest());
  }

  @Test
  public void generateMetadataIsCalledIfRepoIsNotScheduled() throws Exception {
    when(repoService.isRepoScheduled(ANY_REPONAME)).thenReturn(false);

    performGenerateMetaDataPost()
        .andExpect(status().isCreated());

    verify(metadataService).generateYumMetadata(ANY_REPONAME);
  }
  
  private ResultActions performGenerateMetaDataPost() throws Exception {
    return mockMvc.perform(MockMvcRequestBuilders.post("/repo/" + ANY_REPONAME + "/repodata"));
  }
}
