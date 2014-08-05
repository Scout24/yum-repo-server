package de.is24.infrastructure.gridfs.http.storage;

import de.is24.infrastructure.gridfs.http.mongo.IntegrationTestContext;
import org.junit.ClassRule;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.uniqueRepoName;

public class AbstractStorageServiceIT {

  @ClassRule
  public static IntegrationTestContext context = new IntegrationTestContext();

  protected static final String REPOMD_PATH = "/repodata/repomd.xml";
  protected static final String PRIMARY_XMl_PATH = "/repodata/primary.xml.gz";
  protected static final String METADATA_PATH = "/repodate/generation-metadata.yaml";

  protected String givenFullRepository() throws Exception {
    String repoName = uniqueRepoName();
    context.gridFsService().storeRpm(repoName, getClass().getResourceAsStream("/rpms/valid.noarch.rpm"));
    storeFile(repoName, REPOMD_PATH);
    storeFile(repoName, PRIMARY_XMl_PATH);
    storeFile(repoName, METADATA_PATH);
    return repoName;
  }

  private void storeFile(String repoName, String path) {
    context.fileStorageService().storeFile(contentInputStream(), new FileDescriptor(repoName + path));
  }

  protected ByteArrayInputStream contentInputStream() {
    return new ByteArrayInputStream("Content".getBytes());
  }

  protected FileStorageItem givenFileWithDescriptor(FileDescriptor descriptor) throws IOException {
    return context.fileStorageService().storeFile(contentInputStream(), descriptor);
  }
}
