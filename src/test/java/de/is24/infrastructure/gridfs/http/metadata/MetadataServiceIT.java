package de.is24.infrastructure.gridfs.http.metadata;

import de.is24.infrastructure.gridfs.http.category.LocalExecutionOnly;
import de.is24.infrastructure.gridfs.http.domain.RepoEntry;
import de.is24.infrastructure.gridfs.http.mongo.IntegrationTestContext;
import de.is24.infrastructure.gridfs.http.storage.FileDescriptor;
import de.is24.infrastructure.gridfs.http.storage.FileStorageItem;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.uniqueRepoName;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.COMPLEX_RPM_FILE_NAME;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.streamOf;
import static java.io.File.createTempFile;
import static java.lang.System.currentTimeMillis;
import static org.apache.commons.lang.time.DateUtils.addHours;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@Category(LocalExecutionOnly.class)
public class MetadataServiceIT {
  @ClassRule
  public static IntegrationTestContext context = new IntegrationTestContext();

  private MetadataService service;
  private String reponame;

  @Before
  public void setUp() throws Exception {
    service = context.metadataService();
    reponame = uniqueRepoName();
  }

  @Test
  public void generateYumRepoMetadata() throws Exception {
    long startTime = currentTimeMillis();
    context.gridFsService().storeRpm(reponame, streamOf(COMPLEX_RPM_FILE_NAME));
    service.generateYumMetadataIfNecessary(reponame);
    assertDbFile("primary");
    assertDbFile("other");
    assertDbFile("filelists");
    assertRepoMdXml();
    assertRepoMdXmlSignature();

    final RepoEntry repoEntry = context.repoEntriesRepository().findFirstByName(reponame);
    assertThat(repoEntry.getLastMetadataGeneration().getTime(),
      greaterThan(startTime));
    assertThat(repoEntry.getHashOfEntries(), is(notNullValue()));
  }

  @Test
  public void doNotGenerateMetadataIfNoRepoModificationWasMade() throws Exception {
    String reponame = uniqueRepoName();
    context.repoService().createOrUpdate(reponame);

    RepoEntry repoEntryBefore = context.repoEntriesRepository().findFirstByName(reponame);
    repoEntryBefore.setHashOfEntries(context.entriesHashCalculator().hashForRepo(reponame));
    repoEntryBefore.setLastMetadataGeneration(new Date());
    context.repoEntriesRepository().save(repoEntryBefore);

    service.generateYumMetadataIfNecessary(reponame);

    RepoEntry repoEntryAfter = context.repoEntriesRepository().findFirstByName(reponame);
    assertThat(repoEntryAfter, is(repoEntryBefore));
  }

  @Test
  public void cleanupOldMetadata() throws Exception {
    String reponame = uniqueRepoName();

    Object sqliteFileId = givenSomeSqliteFileFromOneHourAgo(reponame);

    context.gridFsService().storeRpm(reponame, streamOf(COMPLEX_RPM_FILE_NAME));
    service.generateYumMetadataIfNecessary(reponame);

    assertTrue(context.fileStorageService().findById(sqliteFileId).isMarkedAsDeleted());
  }

  private Object givenSomeSqliteFileFromOneHourAgo(String reponame) throws IOException {
    String givenSqliteFilenameInfix = "other-123";
    context.gridFsService()
    .storeRepodataDbBz2(reponame, createTempFile(reponame, "dummyfile"), givenSqliteFilenameInfix);

    FileStorageItem file = findSqLiteFile(reponame, givenSqliteFilenameInfix);
    assertThat(file, notNullValue());
    context.fileStorageService().setUploadDate(file, addHours(new Date(), -1));
    return file.getId();
  }

  private FileStorageItem findSqLiteFile(String reponame,
                                         String givenSqliteFilenameInfix) {
    List<FileStorageItem> storageItems = context.fileStorageService()
      .findByPrefix(reponame + "/repodata/" + givenSqliteFilenameInfix);
    return (!storageItems.isEmpty()) ? storageItems.get(0) : null;
  }

  private void assertRepoMdXml() {
    FileDescriptor descriptor = new FileDescriptor(reponame, "repodata", "repomd.xml");

    FileStorageItem storageItem = context.fileStorageService().findBy(descriptor);
    assertThat(storageItem, notNullValue());
  }

  private void assertRepoMdXmlSignature() {
    FileDescriptor descriptor = new FileDescriptor(reponame, "repodata", "repomd.xml.asc");

    FileStorageItem storageItem = context.fileStorageService().findBy(descriptor);
    assertThat(storageItem, notNullValue());
  }

  private void assertDbFile(String type) {
    FileStorageItem dbFile = context.fileStorageService().findByPrefix(reponame + "/repodata/" + type).get(0);
    String sha256 = dbFile.getChecksumSha256();
    assertThat(dbFile.getFilename(), endsWith(type + "-" + sha256 + ".sqlite.bz2"));
    assertFalse(dbFile.isMarkedAsDeleted());
  }

}
