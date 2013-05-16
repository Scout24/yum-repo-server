package de.is24.infrastructure.gridfs.http.metadata;

import com.mongodb.gridfs.GridFSDBFile;
import de.is24.infrastructure.gridfs.http.jaxb.Data;
import de.is24.infrastructure.gridfs.http.gridfs.GridFsService;
import de.is24.infrastructure.gridfs.http.metadata.generation.RepoMdGenerator;
import de.is24.infrastructure.gridfs.http.repos.RepoCleaner;
import de.is24.infrastructure.gridfs.http.repos.RepoService;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import static org.apache.commons.lang.time.DateUtils.addMinutes;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class MetadataServiceTest {
  private static final String REGEX_REPODATA_SQLITE_FILES = "/repodata/.*sqlite.bz2";
  private static final int OUTDATED_META_DATA_SURVIVAL_TIME = 5;

  private MetadataService service;
  private String reponame;
  private GridFsService gridFsService;
  private RepoCleaner repoCleaner;

  @Before
  public void setup() throws Exception {
    this.gridFsService = mock(GridFsService.class);
    when(gridFsService.storeRepodataDbBz2(anyString(), any(File.class), anyString())).thenReturn(new Data());
    this.repoCleaner = mock(RepoCleaner.class);

    RepoService repoService = mock(RepoService.class);
    when(repoService.needsMetadataUpdate(anyString())).thenReturn(true);
    this.service = new MetadataService(gridFsService, mock(YumEntriesRepository.class), mock(RepoMdGenerator.class),
      repoService, repoCleaner);
    this.service.setOutdatedMetaDataSurvivalTime(OUTDATED_META_DATA_SURVIVAL_TIME);
    this.reponame = "any-reponame";
  }


  @Test
  public void outdatedMetaDataFilesAreDeleted() throws Exception {
    Date beforeDate = addMinutes(new Date(), -4);
    ArrayList<GridFSDBFile> expectedFilesToDelete = new ArrayList<>(0);

    when(gridFsService.findByFilenamePatternAndBeforeUploadDate(anyString(), any(Date.class))).thenReturn(
      expectedFilesToDelete);

    this.service.generateYumMetadata(reponame);

    verify(gridFsService).findByFilenamePatternAndBeforeUploadDate(eq(reponame + REGEX_REPODATA_SQLITE_FILES),
      argThat(lessThan(beforeDate)));
    verify(gridFsService).delete(expectedFilesToDelete);
  }

  @Test
  public void cleanRepositoryBeforeGeneration() throws Exception {
    this.service.generateYumMetadata(reponame);
    verify(repoCleaner).cleanup(eq(reponame));
  }
}
