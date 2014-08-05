package de.is24.infrastructure.gridfs.http.storage;

import de.is24.infrastructure.gridfs.http.gridfs.GridFsService;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.uniqueRepoName;

public class StorageTestUtils {

  private final GridFsService gridFsService;
  private final FileStorageService fileStorageService;

  public StorageTestUtils(GridFsService gridFsService, FileStorageService fileStorageService) {
    this.gridFsService = gridFsService;
    this.fileStorageService = fileStorageService;
  }

  public static final String REPOMD_PATH = "/repodata/repomd.xml";
  public static final String PRIMARY_XMl_PATH = "/repodata/primary.xml.gz";
  public static final String METADATA_PATH = "/repodate/generation-metadata.yaml";

  public String givenFullRepository() throws Exception {
    String repoName = uniqueRepoName();
    gridFsService.storeRpm(repoName, getClass().getResourceAsStream("/rpms/valid.noarch.rpm"));
    storeFile(repoName, REPOMD_PATH);
    storeFile(repoName, PRIMARY_XMl_PATH);
    storeFile(repoName, METADATA_PATH);
    return repoName;
  }

  private void storeFile(String repoName, String path) {
    fileStorageService.storeFile(contentInputStream(), new FileDescriptor(repoName + path));
  }

  public ByteArrayInputStream contentInputStream() {
    return new ByteArrayInputStream("Content".getBytes());
  }

  public FileStorageItem givenFileWithDescriptor(FileDescriptor descriptor) throws IOException {
    return fileStorageService.storeFile(contentInputStream(), descriptor);
  }
}
