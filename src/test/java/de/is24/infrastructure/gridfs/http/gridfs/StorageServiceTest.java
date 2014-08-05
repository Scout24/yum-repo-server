package de.is24.infrastructure.gridfs.http.gridfs;

import de.is24.infrastructure.gridfs.http.domain.RepoEntry;
import de.is24.infrastructure.gridfs.http.domain.YumEntry;
import de.is24.infrastructure.gridfs.http.exception.BadRequestException;
import de.is24.infrastructure.gridfs.http.exception.GridFSFileNotFoundException;
import de.is24.infrastructure.gridfs.http.exception.RepositoryIsUndeletableException;
import de.is24.infrastructure.gridfs.http.metadata.YumEntriesRepository;
import de.is24.infrastructure.gridfs.http.repos.RepoService;
import de.is24.infrastructure.gridfs.http.storage.FileDescriptor;
import de.is24.infrastructure.gridfs.http.storage.FileStorageItem;
import de.is24.infrastructure.gridfs.http.storage.FileStorageService;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;

import static de.is24.infrastructure.gridfs.http.domain.RepoType.SCHEDULED;
import static de.is24.infrastructure.gridfs.http.domain.RepoType.STATIC;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class StorageServiceTest {
  private StorageService service;
  private FileStorageService fileStorageService;

  private RepoService repoService;

  @Before
  public void setUp() {
    YumEntriesRepository yumEntriesRepository = mock(YumEntriesRepository.class);
    when(yumEntriesRepository.findOne(any(ObjectId.class))).thenReturn(new YumEntry(null, null, null));

    repoService = mock(RepoService.class);
    fileStorageService = mock(FileStorageService.class);
    service = new StorageService(fileStorageService, yumEntriesRepository, repoService);
  }

  @Test(expected = BadRequestException.class)
  public void throwExceptionForSourceNull() throws Exception {
    service.propagateRpm(null, "dest-repo");
  }

  @Test(expected = BadRequestException.class)
  public void throwExceptionForDestRepoNull() throws Exception {
    service.propagateRpm("repo/arch/file.rpm", null);
  }

  @Test(expected = BadRequestException.class)
  public void throwExceptionForInvalidSourcePathDepth() throws Exception {
    service.propagateRpm("to-low-path/file.rpm", "dest-repo");
  }

  @Test(expected = GridFSFileNotFoundException.class)
  public void throwExceptionForNotRpmSourceFile() throws Exception {
    service.propagateRpm("repo/arch/file.txt", "dest-repo");
  }

  @Test(expected = BadRequestException.class)
  public void throwExceptionForInvalidDestRepo() throws Exception {
    service.propagateRpm("repo/arch/file.rpm", "dest-repo/with-more-depth");
  }

  @Test(expected = GridFSFileNotFoundException.class)
  public void throwExceptionForFileNotFound() throws Exception {
    service.propagateRpm("repo/arch/file.rpm", "dest-repo");
  }

  @Test
  public void moveFileToNewRepo() throws Exception {
    FileStorageItem storageItem = mock(FileStorageItem.class);
    when(storageItem.getFilename()).thenReturn("repo/arch/file.rpm");
    when(fileStorageService.findBy(any(FileDescriptor.class))).thenReturn(storageItem);

    service.propagateRpm("repo/arch/file.rpm", "dest-repo");

    verify(fileStorageService).moveTo(eq(storageItem), eq("dest-repo"));
  }

  @Test(expected = RepositoryIsUndeletableException.class)
  public void failOnDeleteForUndeletableRepository() throws Exception {
    RepoEntry repoEntry = new RepoEntry();
    repoEntry.setUndeletable(true);
    when(repoService.ensureEntry("repo", STATIC, SCHEDULED)).thenReturn(repoEntry);
    service.deleteRepository("repo");
  }

  @Test(expected = BadRequestException.class)
  public void failOnPropagationOfRpmToSameRepo() throws Exception {
    service.propagateRpm("repo/arch/file.rpm", "repo");
  }

  @Test(expected = BadRequestException.class)
  public void failOnPropagationOfRepositoryToSameRepo() throws Exception {
    service.propagateRepository("repo", "repo");
  }
}
