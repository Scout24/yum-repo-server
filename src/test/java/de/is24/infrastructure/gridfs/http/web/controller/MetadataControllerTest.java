package de.is24.infrastructure.gridfs.http.web.controller;

import de.is24.infrastructure.gridfs.http.exception.BadRequestException;
import de.is24.infrastructure.gridfs.http.metadata.MetadataService;
import de.is24.infrastructure.gridfs.http.repos.RepoService;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class MetadataControllerTest {
  private static final String ANY_REPONAME = "any-reponame";
  private MetadataController controller;
  private MetadataService metadataService;
  private RepoService repoService;

  @Before
  public void setup() {
    this.metadataService = mock(MetadataService.class);
    this.repoService = mock(RepoService.class);
    this.controller = new MetadataController(metadataService, repoService);
  }

  @Test(expected = BadRequestException.class)
  public void throwsBadRequestExceptionIfRepoIsScheduled() throws Exception {
    when(repoService.isRepoScheduled(ANY_REPONAME)).thenReturn(true);

    controller.generate(ANY_REPONAME);
  }

  @Test
  public void generateMetadataIsCalledIfRepoIsNotScheduled() throws Exception {
    when(repoService.isRepoScheduled(ANY_REPONAME)).thenReturn(false);

    controller.generate(ANY_REPONAME);

    verify(metadataService).generateYumMetadata(ANY_REPONAME);
  }
}
