package de.is24.infrastructure.gridfs.http.gridfs;

import com.mongodb.BasicDBObject;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSFile;
import de.is24.infrastructure.gridfs.http.category.LocalExecutionOnly;
import de.is24.infrastructure.gridfs.http.domain.YumEntry;
import de.is24.infrastructure.gridfs.http.exception.BadRequestException;
import de.is24.infrastructure.gridfs.http.exception.GridFSFileAlreadyExistsException;
import de.is24.infrastructure.gridfs.http.exception.GridFSFileNotFoundException;
import de.is24.infrastructure.gridfs.http.exception.InvalidRpmHeaderException;
import de.is24.infrastructure.gridfs.http.jaxb.Data;
import de.is24.infrastructure.gridfs.http.mongo.IntegrationTestContext;
import de.is24.infrastructure.gridfs.http.storage.FileStorageItem;
import org.apache.commons.lang.time.DateUtils;
import org.bson.types.ObjectId;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import static com.mongodb.gridfs.GridFSUtil.mergeMetaData;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.MARKED_AS_DELETED_KEY;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.REPO_KEY;
import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.uniqueRepoName;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.RPM_FILE_SIZE;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.streamOf;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.gridfs.GridFsCriteria.whereFilename;
import static org.springframework.data.mongodb.gridfs.GridFsCriteria.whereMetaData;


@Category(LocalExecutionOnly.class)
public class GridFsServiceIT {
  public static final String INVALIDE_REPO_NAME = "hall//fdg";
  public static final String REPO = "repo";
  public static final String ARCH = "arch";
  public static final String NOARCH = "noarch";
  public static final String SRC = "src";
  public static final String SHA256 = "sha256";
  public static final String TEST_FILE_OPEN_SHA256 = "068b5ccfeff0d2d4b3d792336f7dffcc0a9f7f82b81435940707014807e0fd2b";
  public static final String TEST_FILE_BZ2_SHA256 = "6e2b1e4a5d95b2c0cdec718fa381a41d159750e7a5285a6eff4a4207589c2dc0";
  private static final String UPLOAD_DATE = "uploadDate";
  public static final String TESTING_ARCH = "testing";
  private static final String REPODATA = "repodata";

  @ClassRule
  public static IntegrationTestContext context = new IntegrationTestContext();


  public static final String INVALID_RPM = "invalid.rpm";
  private static final String VALID_NOARCH_RPM = "valid.noarch.rpm";
  private static final String VALID_SOURCE_RPM = "valid.src.rpm";
  private static final String VALID_NOARCH_RPM_SHA256 =
    "70506d08285fa0c0120ccd4a1a8fe5537c41838b08655ac4c8dc9de8afb16a2f";
  private static final String VALID_SOURCE_RPM_SHA256 =
    "e687aee5c288062f9f79fd18308ff52f6fb46df38d768d9c71f037f8c8833394";
  private static final String REPOMD_PATH = "/repodata/repomd.xml";
  private static final String PRIMARY_XMl_PATH = "/repodata/primary.xml.gz";
  private static final String METADATA_PATH = "/generation-metadata.yaml";
  private static final String VALID_FILENAME_WITHOUT_VERSION = "test-artifact";
  private static final String VALID_NOARCH_RPM_PATH_WITHOUT_VERSION = "/" + NOARCH + "/" +
    VALID_FILENAME_WITHOUT_VERSION;
  public static final String NOARCH_RPM_VERSION = "-1.2-1.noarch.rpm";

  private static final String VALID_FILENAME = VALID_FILENAME_WITHOUT_VERSION + NOARCH_RPM_VERSION;
  private static final String VALID_NOARCH_RPM_PATH = VALID_NOARCH_RPM_PATH_WITHOUT_VERSION + NOARCH_RPM_VERSION;
  private static final String VALID_SOURCE_RPM_PATH = "/src/yum-repo-client-1.1-273.src.rpm";
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
    String sourceRepo = givenFullRepository();
    String destinationRepo = givenFullRepository();
    startTime = currentTimeMillis();

    context.gridFsService().propagateRpm(sourceRepo + rpmPathInRepo, destinationRepo);

    assertOldRpmIsOverwritten(destinationRepo);

    GridFSDBFile rpm = findNoarchRpm(destinationRepo);
    assertThat(rpm, notNullValue());
    assertFileMetaData(destinationRepo, rpm);
    assertYumEntryHas(rpm, destinationRepo);
    assertRepoWasModifiedAfterStartTime(sourceRepo);
    assertRepoWasModifiedAfterStartTime(destinationRepo);
  }

  @Test
  public void propagateRepository() throws Exception {
    String sourceRepo = givenFullRepository();
    String destinationRepo = givenFullRepository();
    startTime = currentTimeMillis();

    context.gridFsService().propagateRepository(sourceRepo, destinationRepo);

    assertOldRpmIsOverwritten(destinationRepo);
    assertRepoMetadataExists(sourceRepo);

    GridFSDBFile rpm = findNoarchRpm(destinationRepo);
    assertThat(rpm, notNullValue());
    assertFileMetaData(destinationRepo, rpm);
    assertYumEntryHas(rpm, destinationRepo);
    assertRepoMetadataExists(destinationRepo);
    assertRepoWasModifiedAfterStartTime(sourceRepo);
    assertRepoWasModifiedAfterStartTime(destinationRepo);
  }

  @Test
  public void propagateRepositoryWithDeletedFiles() throws Exception {
    String sourceRepo = givenFullRepository();
    GridFsFileDescriptor descriptor = new GridFsFileDescriptor(sourceRepo, NOARCH, "myDeleteFile.rpm");
    givenFileToBeDeleted(descriptor, new Date());

    String destinationRepo = givenFullRepository();
    startTime = currentTimeMillis();

    context.gridFsService().propagateRepository(sourceRepo, destinationRepo);

    assertOldRpmIsOverwritten(destinationRepo);
    assertRepoMetadataExists(sourceRepo);

    GridFSDBFile rpm = findNoarchRpm(destinationRepo);
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

    GridFSDBFile file = findNoarchRpm(reponame);
    assertThat(file, notNullValue());
    assertFileMetaData(reponame, file);
    assertThat((String) file.getMetaData().get(SHA256), is(VALID_NOARCH_RPM_SHA256));
    assertThat(file.get(UPLOAD_DATE), notNullValue());
    assertThat(file.getContentType(), is("application/x-rpm"));
    assertThat(file.getLength(), is((long) RPM_FILE_SIZE));
    assertYumEntryForValidNoArchRpm(reponame, file);
    assertRepoWasModifiedAfterStartTime(reponame);
  }

  @Test
  public void regenerateAllFileMetaData() throws Exception {
    String reponame = uniqueRepoName();
    context.gridFsService().storeRpm(reponame, streamOf(VALID_NOARCH_RPM));

    GridFSDBFile file = findNoarchRpm(reponame);
    context.yumEntriesRepository().delete((ObjectId) file.getId());

    context.gridFsService().regenerateMetadataForAllFiles();

    assertYumEntryForValidNoArchRpm(reponame, file);
  }

  @Test
  public void storeValidSourceRpm() throws Exception {
    startTime = currentTimeMillis();

    String reponame = uniqueRepoName();

    context.gridFsService().storeRpm(reponame, streamOf(VALID_SOURCE_RPM));

    GridFSDBFile file = context.gridFsTemplate().findOne(query(whereFilename().is(reponame + VALID_SOURCE_RPM_PATH)));
    assertThat(file, notNullValue());
    assertThat((String) file.getMetaData().get(REPO), is(reponame));
    assertThat((String) file.getMetaData().get(ARCH), is(SRC));
    assertThat((String) file.getMetaData().get(SHA256), is(VALID_SOURCE_RPM_SHA256));
    assertThat(file.getContentType(), is("application/x-rpm"));
    assertThat(file.getLength(), is(9644L));

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
    String reponame = givenFullRepository();
    context.gridFsService().storeRpm(reponame, streamOf(VALID_SOURCE_RPM));
    context.gridFsService().deleteRepository(reponame);

    assertThatFileIsMarkedForDeletion(createValidNoarchRPMDescriptorInRepo(reponame));
    assertThat(context.yumEntriesRepository().findByRepo(reponame).size(), is(0));
    assertThat(context.repoEntriesRepository().findFirstByName(reponame), nullValue());
  }

  private GridFsFileDescriptor createValidNoarchRPMDescriptorInRepo(String reponame) {
    return new GridFsFileDescriptor(reponame, NOARCH, VALID_FILENAME);
  }

  @Test
  public void deleteRpm() throws Exception {
    String reponame = givenFullRepository();
    startTime = currentTimeMillis();

    GridFsFileDescriptor descriptor = new GridFsFileDescriptor(reponame, NOARCH,
      VALID_FILENAME_WITHOUT_VERSION + NOARCH_RPM_VERSION);
    context.gridFsService().delete(descriptor);
    assertThat(context.gridFsService().findFileByDescriptor(descriptor), nullValue());
    assertThat(context.yumEntriesRepository().findByRepo(reponame).size(), is(0));
    assertRepoWasModifiedAfterStartTime(reponame);
  }

  @Test
  public void metaDataForDeletionIsSetByFilenameRegex() throws Exception {
    final String reponame = uniqueRepoName();
    GridFsFileDescriptor matchingDescriptor = new GridFsFileDescriptor(reponame, TESTING_ARCH,
      "willmatch-filename.rpm");
    givenFileWithDescriptor(matchingDescriptor);


    GridFsFileDescriptor noMatchDescriptor = new GridFsFileDescriptor(reponame, TESTING_ARCH, "no_match");
    givenFileWithDescriptor(noMatchDescriptor);

    context.gridFsService().markForDeletionByFilenameRegex(".*-filename");

    assertThatFileIsMarkedForDeletion(matchingDescriptor);
    FileStorageItem storageItem = context.gridFsService().findFileByDescriptor(noMatchDescriptor);
    assertThat(((GridFsFileStorageItem) storageItem).getDbFile()
            .getMetaData().get(MARKED_AS_DELETED_KEY),
      is(nullValue()));
  }

  @Test
  public void metaDataForDeletionIsSetByPath() throws Exception {
    final String repoName = givenFullRepository();
    GridFsFileDescriptor descriptor = createValidNoarchRPMDescriptorInRepo(repoName);

    context.gridFsService().markForDeletionByPath(descriptor.getPath());

    assertThatFileIsMarkedForDeletion(descriptor);
  }

  private void assertThatFileIsMarkedForDeletion(GridFsFileDescriptor descriptor) {
    FileStorageItem storageItem = context.gridFsService().findFileByDescriptor(descriptor);
    final Date deletionObject = storageItem.getDateOfMarkAsDeleted();
    assertThat(deletionObject, is(notNullValue()));
    assertThat(deletionObject, is(instanceOf(Date.class)));

    final Date deletionTime = (Date) deletionObject;
    assertThat(deletionTime, greaterThanOrEqualTo(testStart));
    assertThat(deletionTime, lessThanOrEqualTo(new Date()));
  }

  @Test
  public void metaDataForDeletionIsSetById() throws Exception {
    final String repoName = givenFullRepository();
    GridFsFileDescriptor descriptor = createValidNoarchRPMDescriptorInRepo(repoName);
    final FileStorageItem fileToMarkAsDeleted = context.gridFsService().findFileByDescriptor(descriptor);

    context.gridFsService().markForDeletionById((ObjectId) fileToMarkAsDeleted.getId());

    assertThatFileIsMarkedForDeletion(descriptor);
  }

  @Test
  public void metaDataForDeletionIsSetOnlyOnce() throws Exception {
    final String repoName = givenFullRepository();
    final Date yesterday = DateUtils.addDays(new Date(), -1);
    final String file = "a_file_to_be_deleted";
    GridFsFileDescriptor descriptor = new GridFsFileDescriptor(repoName, TESTING_ARCH, file);
    givenFileToBeDeleted(descriptor, yesterday);

    context.gridFsService().markForDeletionByPath(descriptor.getPath());

    final FileStorageItem storageItem = context.fileStorageService().findBy(descriptor);

    assertThat(storageItem.getDateOfMarkAsDeleted(), is(equalTo(yesterday)));
  }

  @Test
  public void deleteFilesMarkedAsDeleted() throws Exception {
    final Date now = new Date();
    final String nothingToDeleteRepo = givenFullRepository();
    givenTowOfThreeFilesToBeDeleted(now);

    context.fileStorageService().removeFilesMarkedAsDeletedBefore(now);

    final List<GridFSDBFile> fileList = context.gridFsTemplate()
      .find(query(whereMetaData(MARKED_AS_DELETED_KEY).ne(null)));
    assertThat(fileList.size(), is(1));

    final List<GridFSDBFile> filesInNothingToDeleteRepo = context.gridFsTemplate()
      .find(query(whereMetaData(REPO_KEY).is(nothingToDeleteRepo)));
    assertThat(filesInNothingToDeleteRepo.size(), is(4));
  }

  private void givenTowOfThreeFilesToBeDeleted(final Date now) throws IOException {
    final String repoToDeleteIn = uniqueRepoName();
    final Date past = DateUtils.addDays(now, -1);
    givenFileToBeDeleted(new GridFsFileDescriptor(repoToDeleteIn, TESTING_ARCH, "toBeDeletedPast1"), past);
    givenFileToBeDeleted(new GridFsFileDescriptor(repoToDeleteIn, TESTING_ARCH, "toBeDeletedPast2"), past);
    givenFileToBeDeleted(new GridFsFileDescriptor(repoToDeleteIn, TESTING_ARCH, "toBeDeletedFuture"),
      DateUtils.addDays(now, 1));
  }

  private GridFSFile givenFileToBeDeleted(GridFsFileDescriptor descriptor, final Date time) throws IOException {
    final GridFSDBFile toBeDeleted = ((GridFsFileStorageItem) givenFileWithDescriptor(descriptor)).getDbFile();

    mergeMetaData(toBeDeleted, new BasicDBObject(MARKED_AS_DELETED_KEY, time));
    toBeDeleted.save();
    return toBeDeleted;
  }

  private FileStorageItem givenFileWithDescriptor(GridFsFileDescriptor descriptor) throws IOException {
    return context.fileStorageService().storeFile(contentInputStream(), descriptor);
  }


  @Test
  public void deleteFiles() throws Exception {
    String repo = givenFullRepository();
    GridFsFileDescriptor descriptor = new GridFsFileDescriptor(repo, TESTING_ARCH, "any-filename");

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

    GridFsFileDescriptor descriptor = new GridFsFileDescriptor(reponame, REPODATA,
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
    GridFSDBFile file = findNoarchRpm(sourceRepo);
    context.gridFsService().markForDeletionById((ObjectId) file.getId());

    context.gridFsService().propagateRpm(file.getFilename(), destinationRepo);
  }

  private void assertRepoWasModifiedAfterStartTime(String reponame) {
    assertThat(context.repoEntriesRepository().findFirstByName(reponame).getLastModified().getTime(),
      greaterThan(startTime));
  }

  private void assertYumEntryForValidNoArchRpm(String reponame, GridFSDBFile file) {
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

  private String givenFullRepository() throws Exception {
    String repoName = uniqueRepoName();
    context.gridFsService().storeRpm(repoName, getClass().getResourceAsStream("/rpms/valid.noarch.rpm"));
    storeFile(repoName, REPOMD_PATH);
    storeFile(repoName, PRIMARY_XMl_PATH);
    storeFile(repoName, METADATA_PATH);
    return repoName;
  }

  private void storeFile(String repoName, String path) {
    GridFSFile file = context.gridFsTemplate().store(contentInputStream(), repoName + path);
    BasicDBObject dbObject = new BasicDBObject();
    dbObject.put(REPO, repoName);
    dbObject.put(ARCH, "repodata");
    file.setMetaData(dbObject);
    file.save();
  }

  private ByteArrayInputStream contentInputStream() {
    return new ByteArrayInputStream("Content".getBytes());
  }

  private void assertOldRpmIsOverwritten(String destinationRepo) {
    assertThat(context.gridFsTemplate()
      .find(query(whereFilename().is(destinationRepo + VALID_NOARCH_RPM_PATH)))
      .size(), is(1));
  }

  private void assertFileMetaData(String destinationRepo, GridFSDBFile rpm) {
    assertThat((String) rpm.getMetaData().get(REPO), is(destinationRepo));
    assertThat((String) rpm.getMetaData().get(ARCH), is(NOARCH));
  }

  private void assertYumEntryHas(GridFSDBFile rpm, String destinationRepo) {
    YumEntry entry = context.yumEntriesRepository().findOne((ObjectId) rpm.getId());
    assertThat(entry.getRepo(), is(destinationRepo));
  }

  private void assertRepoMetadataExists(String sourceRepo) {
    assertThat(context.gridFsTemplate().findOne(query(whereFilename().is(sourceRepo + REPOMD_PATH))), notNullValue());
    assertThat(context.gridFsTemplate().findOne(query(whereFilename().is(sourceRepo + PRIMARY_XMl_PATH))),
      notNullValue());
    assertThat(context.gridFsTemplate().findOne(query(whereFilename().is(sourceRepo + METADATA_PATH))), notNullValue());
  }

  private GridFSDBFile findNoarchRpm(String destinationRepo) {
    return context.gridFsTemplate().findOne(query(whereFilename().is(destinationRepo + VALID_NOARCH_RPM_PATH)));
  }

  private InputStream asStream(String resource) {
    return getClass().getResourceAsStream(resource);
  }
}
