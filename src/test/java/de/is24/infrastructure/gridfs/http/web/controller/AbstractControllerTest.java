package de.is24.infrastructure.gridfs.http.web.controller;

import de.is24.infrastructure.gridfs.http.gridfs.GridFsService;
import de.is24.infrastructure.gridfs.http.metadata.MetadataService;
import de.is24.infrastructure.gridfs.http.repos.RepoService;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@RunWith(MockitoJUnitRunner.class)
public abstract class AbstractControllerTest {

  @Mock
  protected GridFsService gridFsService;
  @Mock
  protected MetadataService metadataService;
  @Mock
  protected RepoService repoService;
  
  protected MockMvc mockMvc;

  @Before
  public void setUp() throws Exception {
    mockMvc = standaloneSetup(
                new FileController(gridFsService),
                new MetadataController(metadataService, repoService),
                new RepositoryController(gridFsService, repoService),
                new PropagationController(gridFsService)
    ).build();
  }

  protected ResultActions performSimpleGet(final String url) throws Exception {
    return mockMvc.perform(MockMvcRequestBuilders.get(url));
  }

  protected ResultMatcher badRangeStatus() {
    return status().is(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value());
  }
}
