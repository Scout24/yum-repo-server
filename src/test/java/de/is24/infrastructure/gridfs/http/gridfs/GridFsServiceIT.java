package de.is24.infrastructure.gridfs.http.gridfs;

import com.mongodb.BasicDBObject;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSFile;
import com.mongodb.gridfs.GridFSUtil;
import de.is24.infrastructure.gridfs.http.category.LocalExecutionOnly;
import de.is24.infrastructure.gridfs.http.domain.YumEntry;
import de.is24.infrastructure.gridfs.http.exception.BadRequestException;
import de.is24.infrastructure.gridfs.http.exception.GridFSFileAlreadyExistsException;
import de.is24.infrastructure.gridfs.http.exception.InvalidRpmHeaderException;
import de.is24.infrastructure.gridfs.http.jaxb.Data;
import de.is24.infrastructure.gridfs.http.mongo.IntegrationTestContext;
import org.apache.commons.lang.time.DateUtils;
import org.bson.types.ObjectId;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
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
  public static final String OPEN_SIZE = "open_size";
  public static final String OPEN_SHA256 = "open_sha256";
  private static final String UPLOAD_DATE = "uploadDate";

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
  private static final String VALID_NOARCH_RPM_PATH_WITHOUT_VERSION = "/noarch/test-artifact";
  private static final String VALID_NOARCH_RPM_PATH = VALID_NOARCH_RPM_PATH_WITHOUT_VERSION + "-1.2-1.noarch.rpm";
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
    assertThatFileIsMarkedForDeletion(reponame + VALID_NOARCH_RPM_PATH);
    assertThat(context.yumEntriesRepository().findByRepo(reponame).size(), is(0));
    assertThat(context.repoEntriesRepository().findFirstByName(reponame), nullValue());
  }

  @Test
  public void deleteRpm() throws Exception {
    String reponame = givenFullRepository();
    startTime = currentTimeMillis();

    context.gridFsService().delete(reponame + VALID_NOARCH_RPM_PATH);
    assertThat(context.gridFsService().findFileByPath(reponame + VALID_NOARCH_RPM_PATH), nullValue());
    assertThat(context.yumEntriesRepository().findByRepo(reponame).size(), is(0));
    assertRepoWasModifiedAfterStartTime(reponame);
  }

  @Test
  public void metaDataForDeletionIsSetByFilenameRegex() throws Exception {
    final String reponame = uniqueRepoName();
    final String matchPath = reponame + "willmatch-filename.rpm";
    givenFileWithPath(matchPath);

    final String noMatchPath = reponame + "no_match";
    givenFileWithPath(noMatchPath);

    context.gridFsService().markForDeletionByFilenameRegex(".*-filename");

    assertThatFileIsMarkedForDeletion(matchPath);
    assertThat(context.gridFsService().findFileByPath(noMatchPath).getMetaData().get(MARKED_AS_DELETED_KEY),
      is(nullValue()));
  }

  @Test
  public void metaDataForDeletionIsSetByPath() throws Exception {
    final String repoName = givenFullRepository();
    final String validNoArchRpmPath = repoName + VALID_NOARCH_RPM_PATH;

    context.gridFsService().markForDeletionByPath(validNoArchRpmPath);

    assertThatFileIsMarkedForDeletion(validNoArchRpmPath);
  }

  private void assertThatFileIsMarkedForDeletion(final String validNoArchRpmPath) {
    final GridFSDBFile gridFSDBFile = context.gridFsService().findFileByPath(validNoArchRpmPath);
    final Object deletionObject = gridFSDBFile.getMetaData().get(MARKED_AS_DELETED_KEY);
    assertThat(deletionObject, is(notNullValue()));
    assertThat(deletionObject, is(instanceOf(Date.class)));

    final Date deletionTime = (Date) deletionObject;
    assertThat(deletionTime, greaterThanOrEqualTo(testStart));
    assertThat(deletionTime, lessThanOrEqualTo(new Date()));
  }

  @Test
  public void metaDataForDeletionIsSetById() throws Exception {
    final String repoName = givenFullRepository();
    final String validNoArchRpmPath = repoName + VALID_NOARCH_RPM_PATH;
    final GridFSDBFile fileToMarkAsDeleted = context.gridFsService().findFileByPath(validNoArchRpmPath);

    context.gridFsService().markForDeletionById((ObjectId) fileToMarkAsDeleted.getId());

    assertThatFileIsMarkedForDeletion(validNoArchRpmPath);
  }

  @Test
  public void metaDataForDeletionIsSetOnlyOnce() throws Exception {
    final String repoName = givenFullRepository();
    final Date yesterday = DateUtils.addDays(new Date(), -1);
    final String path = repoName + "/a_file_to_be_deleted";
    givenFileToBeDeleted(path, yesterday);

    context.gridFsService().markForDeletionByPath(path);


    final GridFSDBFile dbFile = context.gridFsService().findFileByPath(path);

    assertThat((Date) dbFile.getMetaData().get(MARKED_AS_DELETED_KEY), is(equalTo(yesterday)));
  }

  @Test
  public void deleteFilesMarkedAsDeleted() throws Exception {
    final Date now = new Date();
    final String nothingToDeleteRepo = givenFullRepository();
    givenTowOfThreeFilesToBeDeleted(now);

    context.gridFsService().removeFilesMarkedAsDeletedBefore(now);

    final List<GridFSDBFile> fileList = context.gridFsTemplate()
      .find(query(whereMetaData(MARKED_AS_DELETED_KEY).ne(null)));
    assertThat(fileList.size(), is(1));

    final List<GridFSDBFile> filesInNothingToDeleteRepo = context.gridFsTemplate()
      .find(query(whereMetaData(REPO_KEY).is(nothingToDeleteRepo)));
    assertThat(filesInNothingToDeleteRepo.size(), is(4));
  }

  private void givenTowOfThreeFilesToBeDeleted(final Date now) {
    final Date past = DateUtils.addDays(now, -1);
    givenFileToBeDeleted("toBeDeletedPast1", past);
    givenFileToBeDeleted("toBeDeletedPast2", past);
    givenFileToBeDeleted("toBeDeletedFuture", DateUtils.addDays(now, 1));
  }

  private GridFSFile givenFileToBeDeleted(final String path, final Date time) {
    final GridFSFile toBeDeleted = givenFileWithPath(path);
    GridFSUtil.mergeMetaData(toBeDeleted, new BasicDBObject(MARKED_AS_DELETED_KEY, time));
    toBeDeleted.save();
    return toBeDeleted;
  }

  private GridFSFile givenFileWithPath(String path) {
    return context.gridFsTemplate().store(contentInputStream(), path);
  }


  @Test
  public void deleteFiles() throws Exception {
    String filename = givenFullRepository() + "/any-filename";
    context.gridFsTemplate().store(asStream("/test-for-delete-file.txt"), filename);

    GridFSDBFile file = context.gridFsService().findFileByPath(filename);
    context.gridFsService().delete(asList(file));
    assertThat(context.gridFsService().findFileByPath(filename), nullValue());
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

    GridFSDBFile dbFile = context.gridFsService()
      .findFileByPath(reponame + "/repodata/primary-" + TEST_FILE_BZ2_SHA256 + ".sqlite.bz2");
    assertThat(dbFile, notNullValue());
    assertThat(dbFile.getMetaData().get(REPO).toString(), is(reponame));
    assertThat(dbFile.getMetaData().get(ARCH).toString(), is("repodata"));
    assertThat(dbFile.getLength(), is(68L));
    assertThat(dbFile.getMetaData().get(SHA256).toString(), is(TEST_FILE_BZ2_SHA256));
    assertThat(dbFile.getMetaData().get(OPEN_SIZE).toString(), is("33"));
    assertThat(dbFile.getMetaData().get(OPEN_SHA256).toString(), is(TEST_FILE_OPEN_SHA256));
    assertThat(dbFile.get(UPLOAD_DATE), notNullValue());
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
