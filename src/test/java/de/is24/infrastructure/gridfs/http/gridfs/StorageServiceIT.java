package de.is24.infrastructure.gridfs.http.gridfs;

import de.is24.infrastructure.gridfs.http.category.LocalExecutionOnly;
import de.is24.infrastructure.gridfs.http.domain.YumEntry;
import de.is24.infrastructure.gridfs.http.exception.BadRequestException;
import de.is24.infrastructure.gridfs.http.exception.GridFSFileAlreadyExistsException;
import de.is24.infrastructure.gridfs.http.exception.GridFSFileNotFoundException;
import de.is24.infrastructure.gridfs.http.exception.InvalidRpmHeaderException;
import de.is24.infrastructure.gridfs.http.jaxb.Data;
import de.is24.infrastructure.gridfs.http.mongo.IntegrationTestContext;
import de.is24.infrastructure.gridfs.http.storage.FileDescriptor;
import de.is24.infrastructure.gridfs.http.storage.FileStorageItem;
import org.bson.types.ObjectId;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Date;

import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.MARKED_AS_DELETED_KEY;
import static de.is24.infrastructure.gridfs.http.storage.StorageTestUtils.METADATA_PATH;
import static de.is24.infrastructure.gridfs.http.storage.StorageTestUtils.PRIMARY_XMl_PATH;
import static de.is24.infrastructure.gridfs.http.storage.StorageTestUtils.REPOMD_PATH;
import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.uniqueRepoName;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.RPM_FILE_SIZE;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.streamOf;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.gridfs.GridFsCriteria.whereFilename;


@Category(LocalExecutionOnly.class)
public class StorageServiceIT {
  public static final String INVALIDE_REPO_NAME = "hall//fdg";
  public static final String NOARCH = "noarch";
  public static final String SRC = "src";
  public static final String TEST_FILE_OPEN_SHA256 = "068b5ccfeff0d2d4b3d792336f7dffcc0a9f7f82b81435940707014807e0fd2b";
  public static final String TEST_FILE_BZ2_SHA256 = "6e2b1e4a5d95b2c0cdec718fa381a41d159750e7a5285a6eff4a4207589c2dc0";
  public static final String TESTING_ARCH = "testing";
  private static final String REPODATA = "repodata";

  public static final String INVALID_RPM = "invalid.rpm";
  private static final String VALID_NOARCH_RPM = "valid.noarch.rpm";
  private static final String VALID_SOURCE_RPM = "valid.src.rpm";
  private static final String VALID_NOARCH_RPM_SHA256 =
    "70506d08285fa0c0120ccd4a1a8fe5537c41838b08655ac4c8dc9de8afb16a2f";
  private static final String VALID_SOURCE_RPM_SHA256 =
    "e687aee5c288062f9f79fd18308ff52f6fb46df38d768d9c71f037f8c8833394";
  private static final String VALID_FILENAME_WITHOUT_VERSION = "test-artifact";
  private static final String VALID_NOARCH_RPM_PATH_WITHOUT_VERSION = "/" + NOARCH + "/" +
    VALID_FILENAME_WITHOUT_VERSION;
  public static final String NOARCH_RPM_VERSION = "-1.2-1.noarch.rpm";

  private static final String VALID_FILENAME = VALID_FILENAME_WITHOUT_VERSION + NOARCH_RPM_VERSION;
  private static final String VALID_NOARCH_RPM_PATH = VALID_NOARCH_RPM_PATH_WITHOUT_VERSION + NOARCH_RPM_VERSION;
  private static final String VALID_SOURCE_RPM_PATH = "/src/yum-repo-client-1.1-273.src.rpm";

  @ClassRule
  public static IntegrationTestContext context = new IntegrationTestContext();

  private long startTime = Long.MAX_VALUE;
  private Date testStart = new Date();

  @Test
  public void propagateRpm() throws Exception {
    assertPropagateRpm(VALID_NOARCH_RPM_PATH);
  }

  @Test
  public void propagateRpmWithoutVersion() throws Exception {
    assertPropagateRpm(VALID_NOARCH_RPM_PATH_WITHOUT_VERSION);
  }

  private void assertPropagateRpm(String rpmPathInRepo) throws Exception {
    String sourceRepo = context.storageTestUtils().givenFullRepository();
    String destinationRepo = context.storageTestUtils().givenFullRepository();
    startTime = currentTimeMillis();

    context.gridFsService().propagateRpm(sourceRepo + rpmPathInRepo, destinationRepo);

    assertOldRpmIsOverwritten(destinationRepo);

    FileStorageItem rpm = findNoarchRpm(destinationRepo);
    assertThat(rpm, notNullValue());
    assertFileMetaData(destinationRepo, rpm);
    assertYumEntryHas(rpm, destinationRepo);
    assertRepoWasModifiedAfterStartTime(sourceRepo);
    assertRepoWasModifiedAfterStartTime(destinationRepo);
  }

  @Test
  public void propagateRepository() throws Exception {
    String sourceRepo = context.storageTestUtils().givenFullRepository();
    String destinationRepo = context.storageTestUtils().givenFullRepository();
    startTime = currentTimeMillis();

    context.gridFsService().propagateRepository(sourceRepo, destinationRepo);

    assertOldRpmIsOverwritten(destinationRepo);
    assertRepoMetadataExists(sourceRepo);

    FileStorageItem rpm = findNoarchRpm(destinationRepo);
    assertThat(rpm, notNullValue());
    assertFileMetaData(destinationRepo, rpm);
    assertYumEntryHas(rpm, destinationRepo);
    assertRepoMetadataExists(destinationRepo);
    assertRepoWasModifiedAfterStartTime(sourceRepo);
    assertRepoWasModifiedAfterStartTime(destinationRepo);
  }

  @Test
  public void propagateRepositoryWithDeletedFiles() throws Exception {
    String sourceRepo = context.storageTestUtils().givenFullRepository();
    FileDescriptor descriptor = new FileDescriptor(sourceRepo, NOARCH, "myDeleteFile.rpm");
    context.fileStorageService().markForDeletionByPath(descriptor.getFilename());

    String destinationRepo = context.storageTestUtils().givenFullRepository();
    startTime = currentTimeMillis();

    context.gridFsService().propagateRepository(sourceRepo, destinationRepo);

    assertOldRpmIsOverwritten(destinationRepo);
    assertRepoMetadataExists(sourceRepo);

    FileStorageItem rpm = findNoarchRpm(destinationRepo);
    assertThat(rpm, notNullValue());
    assertFileMetaData(destinationRepo, rpm);
    assertYumEntryHas(rpm, destinationRepo);
    assertRepoMetadataExists(destinationRepo);
    assertRepoWasModifiedAfterStartTime(sourceRepo);
    assertRepoWasModifiedAfterStartTime(destinationRepo);
  }

  @Test(expected = InvalidRpmHeaderException.class)
  public void throwExceptionForEmptyRpm() throws Exception {
    String reponame = uniqueRepoName();

    context.gridFsService().storeRpm(reponame, new ByteArrayInputStream(new byte[0]));
  }

  @Test(expected = InvalidRpmHeaderException.class)
  public void throwExceptionForInvalidRpm() throws Exception {
    String reponame = uniqueRepoName();

    context.gridFsService().storeRpm(reponame, streamOf(INVALID_RPM));
  }

  @Test
  public void storeValidNoarchRpm() throws Exception {
    startTime = currentTimeMillis();

    String reponame = uniqueRepoName();

    context.gridFsService().storeRpm(reponame, streamOf(VALID_NOARCH_RPM));

    FileStorageItem file = findNoarchRpm(reponame);
    assertThat(file, notNullValue());
    assertFileMetaData(reponame, file);
    assertThat(file.getChecksumSha256(), is(VALID_NOARCH_RPM_SHA256));
    assertThat(file.getUploadDate(), notNullValue());
    assertThat(file.getContentType(), is("application/x-rpm"));
    assertThat(file.getSize(), is((long) RPM_FILE_SIZE));
    assertYumEntryForValidNoArchRpm(reponame, file);
    assertRepoWasModifiedAfterStartTime(reponame);
  }

  @Test
  public void regenerateAllFileMetaData() throws Exception {
    String reponame = uniqueRepoName();
    context.gridFsService().storeRpm(reponame, streamOf(VALID_NOARCH_RPM));

    FileStorageItem file = findNoarchRpm(reponame);
    context.yumEntriesRepository().delete((ObjectId) file.getId());

    context.gridFsService().regenerateMetadataForAllFiles();

    assertYumEntryForValidNoArchRpm(reponame, file);
  }

  @Test
  public void storeValidSourceRpm() throws Exception {
    startTime = currentTimeMillis();

    String reponame = uniqueRepoName();

    context.gridFsService().storeRpm(reponame, streamOf(VALID_SOURCE_RPM));

    FileStorageItem file = context.fileStorageService().findBy(new FileDescriptor(reponame + VALID_SOURCE_RPM_PATH));
    assertThat(file, notNullValue());
    assertThat(file.getRepo(), is(reponame));
    assertThat(file.getArch(), is(SRC));
    assertThat(file.getChecksumSha256(), is(VALID_SOURCE_RPM_SHA256));
    assertThat(file.getContentType(), is("application/x-rpm"));
    assertThat(file.getSize(), is(9644L));

    YumEntry yumEntry = context.yumEntriesRepository().findOne((ObjectId) file.getId());
    assertThat(yumEntry.getRepo(), is(reponame));
    assertThat(yumEntry.getYumPackage().getArch(), is(SRC));
    assertThat(yumEntry.getYumPackage().getName(), is("yum-repo-client"));
    assertThat(yumEntry.getYumPackage().getSize().getPackaged(), is(9644));
    assertRepoWasModifiedAfterStartTime(reponame);
  }

  @Test(expected = GridFSFileAlreadyExistsException.class)
  public void forbidOverwritingExistingRpm() throws Exception {
    String reponame = uniqueRepoName();

    context.gridFsService().storeRpm(reponame, streamOf(VALID_NOARCH_RPM));
    context.gridFsService().storeRpm(reponame, streamOf(VALID_NOARCH_RPM));
  }

  @Test(expected = BadRequestException.class)
  public void throwExceptionForBadReponameOnStore() throws Exception {
    String reponame = INVALIDE_REPO_NAME;

    context.gridFsService().storeRpm(reponame, streamOf(VALID_NOARCH_RPM));
  }

  @Test(expected = BadRequestException.class)
  public void throwExceptionForBadReponameOnDelete() throws Exception {
    context.gridFsService().deleteRepository(INVALIDE_REPO_NAME);
  }

  @Test
  public void deleteEmptyRepository() throws Exception {
    String reponame = uniqueRepoName();
    context.gridFsService().deleteRepository(reponame);
    assertThat(context.repoEntriesRepository().findFirstByName(reponame), nullValue());
  }

  @Test
  public void deleteRepository() throws Exception {
    String reponame = context.storageTestUtils().givenFullRepository();
    context.gridFsService().storeRpm(reponame, streamOf(VALID_SOURCE_RPM));
    context.gridFsService().deleteRepository(reponame);

    assertThatFileIsMarkedForDeletion(createValidNoarchRPMDescriptorInRepo(reponame));
    assertThat(context.yumEntriesRepository().findByRepo(reponame).size(), is(0));
    assertThat(context.repoEntriesRepository().findFirstByName(reponame), nullValue());
  }

  private FileDescriptor createValidNoarchRPMDescriptorInRepo(String reponame) {
    return new FileDescriptor(reponame, NOARCH, VALID_FILENAME);
  }

  @Test
  public void deleteRpm() throws Exception {
    String reponame = context.storageTestUtils().givenFullRepository();
    startTime = currentTimeMillis();

    FileDescriptor descriptor = new FileDescriptor(reponame, NOARCH,
      VALID_FILENAME_WITHOUT_VERSION + NOARCH_RPM_VERSION);
    context.gridFsService().delete(descriptor);
    assertThat(context.fileStorageService().findBy(descriptor), nullValue());
    assertThat(context.yumEntriesRepository().findByRepo(reponame).size(), is(0));
    assertRepoWasModifiedAfterStartTime(reponame);
  }

  @Test
  public void metaDataForDeletionIsSetByFilenameRegex() throws Exception {
    final String reponame = uniqueRepoName();
    FileDescriptor matchingDescriptor = new FileDescriptor(reponame, TESTING_ARCH,
      "willmatch-filename.rpm");
    context.storageTestUtils().givenFileWithDescriptor(matchingDescriptor);


    FileDescriptor noMatchDescriptor = new FileDescriptor(reponame, TESTING_ARCH, "no_match");
    context.storageTestUtils().givenFileWithDescriptor(noMatchDescriptor);

    context.fileStorageService().markForDeletionByFilenameRegex(".*-filename");

    assertThatFileIsMarkedForDeletion(matchingDescriptor);
    FileStorageItem storageItem = context.fileStorageService().findBy(noMatchDescriptor);
    assertThat(((GridFsFileStorageItem) storageItem).getDbFile()
            .getMetaData().get(MARKED_AS_DELETED_KEY),
      is(nullValue()));
  }

  @Test
  public void metaDataForDeletionIsSetByPath() throws Exception {
    final String repoName = context.storageTestUtils().givenFullRepository();
    FileDescriptor descriptor = createValidNoarchRPMDescriptorInRepo(repoName);

    context.fileStorageService().markForDeletionByPath(descriptor.getPath());

    assertThatFileIsMarkedForDeletion(descriptor);
  }

  private void assertThatFileIsMarkedForDeletion(FileDescriptor descriptor) {
    FileStorageItem storageItem = context.fileStorageService().findBy(descriptor);
    Date deletionDate = storageItem.getDateOfMarkAsDeleted();
    assertThat(deletionDate, is(notNullValue()));

    assertThat(deletionDate, greaterThanOrEqualTo(testStart));
    assertThat(deletionDate, lessThanOrEqualTo(new Date()));
  }


  @Test
  public void deleteFiles() throws Exception {
    String repo = context.storageTestUtils().givenFullRepository();
    FileDescriptor descriptor = new FileDescriptor(repo, TESTING_ARCH, "any-filename");

    context.gridFsTemplate().store(asStream("/test-for-delete-file.txt"), descriptor.getPath());

    FileStorageItem file = context.fileStorageService().findBy(descriptor);
    context.gridFsService().delete(asList(file));
    assertThat(context.fileStorageService().findBy(descriptor), nullValue());
  }

  @Test
  public void storeRepodataAsBz2() throws Exception {
    String reponame = uniqueRepoName();
    Data data = context.gridFsService()
      .storeRepodataDbBz2(reponame, new File(getClass().getResource("/test-for-bz2.txt").toURI()), "primary");
    assertThat(data.getSize(), is(68L));
    assertThat(data.getChecksum().getChecksum(), is(TEST_FILE_BZ2_SHA256));
    assertThat(data.getOpenSize(), is(33L));
    assertThat(data.getOpenChecksum().getChecksum(), is(TEST_FILE_OPEN_SHA256));

    FileDescriptor descriptor = new FileDescriptor(reponame, REPODATA,
      "primary-" + TEST_FILE_BZ2_SHA256 + ".sqlite.bz2");

    FileStorageItem storageItem = context.fileStorageService().findBy(descriptor);
    assertThat(storageItem, notNullValue());
    assertThat(storageItem.getRepo(), is(reponame));
    assertThat(storageItem.getArch(), is("repodata"));
    assertThat(storageItem.getSize(), is(68L));
    assertThat(storageItem.getChecksumSha256(), is(TEST_FILE_BZ2_SHA256));
    assertThat(storageItem.getUploadDate(), notNullValue());
  }

  @Test(expected = GridFSFileNotFoundException.class)
  public void dontMoveFileMarkedForDeletion() throws Exception {
    String sourceRepo = uniqueRepoName();
    String destinationRepo = uniqueRepoName();
    context.gridFsService().storeRpm(sourceRepo, streamOf(VALID_NOARCH_RPM));
    FileStorageItem file = findNoarchRpm(sourceRepo);
    context.fileStorageService().markForDeletionByPath(file.getFilename());

    context.gridFsService().propagateRpm(file.getFilename(), destinationRepo);
  }

  private void assertRepoWasModifiedAfterStartTime(String reponame) {
    assertThat(context.repoEntriesRepository().findFirstByName(reponame).getLastModified().getTime(),
      greaterThan(startTime));
  }

  private void assertYumEntryForValidNoArchRpm(String reponame, FileStorageItem file) {
    YumEntry yumEntry = context.yumEntriesRepository().findOne((ObjectId) file.getId());
    assertThat(yumEntry, notNullValue());
    assertThat(yumEntry.getRepo(), is(reponame));
    assertThat(yumEntry.getYumPackage().getArch(), is(NOARCH));
    assertThat(yumEntry.getYumPackage().getName(), is("test-artifact"));
    assertThat(yumEntry.getYumPackage().getSize().getPackaged(), is(RPM_FILE_SIZE));
    assertThat(yumEntry.getYumPackage().getChecksum().getType(), is("sha256"));
    assertThat(yumEntry.getYumPackage().getChecksum().getChecksum(), is(VALID_NOARCH_RPM_SHA256));
    assertThat(yumEntry.getYumPackage().getTime().getFile(), notNullValue());
    assertThat(yumEntry.getYumPackage().getPackageFormat().getHeaderStart(), is(280));
    assertThat(yumEntry.getYumPackage().getPackageFormat().getHeaderEnd(), is(1308));
  }

  private void assertOldRpmIsOverwritten(String destinationRepo) {
    assertThat(context.gridFsTemplate()
      .find(query(whereFilename().is(destinationRepo + VALID_NOARCH_RPM_PATH)))
      .size(), is(1));
  }

  private void assertFileMetaData(String destinationRepo, FileStorageItem rpm) {
    assertThat(rpm.getRepo(), is(destinationRepo));
    assertThat(rpm.getArch(), is(NOARCH));
  }

  private void assertYumEntryHas(FileStorageItem rpm, String destinationRepo) {
    YumEntry entry = context.yumEntriesRepository().findOne((ObjectId) rpm.getId());
    assertThat(entry.getRepo(), is(destinationRepo));
  }

  private void assertRepoMetadataExists(String sourceRepo) {
    assertThat(context.gridFsTemplate().findOne(query(whereFilename().is(sourceRepo + REPOMD_PATH))), notNullValue());
    assertThat(context.gridFsTemplate().findOne(query(whereFilename().is(sourceRepo + PRIMARY_XMl_PATH))),
      notNullValue());
    assertThat(context.gridFsTemplate().findOne(query(whereFilename().is(sourceRepo + METADATA_PATH))), notNullValue());
  }

  private FileStorageItem findNoarchRpm(String destinationRepo) {
    return context.fileStorageService().findBy(new FileDescriptor(destinationRepo + VALID_NOARCH_RPM_PATH));
  }

  private InputStream asStream(String resource) {
    return getClass().getResourceAsStream(resource);
  }
}
