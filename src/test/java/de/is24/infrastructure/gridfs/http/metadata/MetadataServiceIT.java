package de.is24.infrastructure.gridfs.http.metadata;

import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.uniqueRepoName;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.COMPLEX_RPM_FILE_NAME;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.streamOf;
import static java.io.File.createTempFile;
import static java.lang.System.currentTimeMillis;
import static java.util.regex.Pattern.compile;
import static org.apache.commons.lang.time.DateUtils.addHours;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.io.IOException;
import java.util.Date;
import de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsCriteria;
import com.mongodb.QueryBuilder;
import com.mongodb.gridfs.GridFSDBFile;
import de.is24.infrastructure.gridfs.http.category.LocalExecutionOnly;
import de.is24.infrastructure.gridfs.http.domain.RepoEntry;
import de.is24.infrastructure.gridfs.http.mongo.IntegrationTestContext;


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
    service.generateYumMetadata(reponame);
    assertDbFile("primary");
    assertDbFile("other");
    assertDbFile("filelists");
    assertRepoMdXml();

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

    service.generateYumMetadata(reponame);

    RepoEntry repoEntryAfter = context.repoEntriesRepository().findFirstByName(reponame);
    assertThat(repoEntryAfter, is(repoEntryBefore));
  }

  @Test
  public void cleanupOldMetadata() throws Exception {
    String reponame = uniqueRepoName();
    service.setOutdatedMetaDataSurvivalTime(5);

    ObjectId sqliteFileId = givenSomeSqliteFileFromOneHourAgo(reponame);

    context.gridFsService().storeRpm(reponame, streamOf(COMPLEX_RPM_FILE_NAME));
    service.generateYumMetadata(reponame);

    assertThat(context.gridFs().findOne(sqliteFileId).getMetaData().get(DatabaseStructure.MARKED_AS_DELETED_KEY),
      is(notNullValue()));
  }

  private ObjectId givenSomeSqliteFileFromOneHourAgo(String reponame) throws IOException {
    String givenSqliteFilenameInfix = "other-123";
    context.gridFsService()
    .storeRepodataDbBz2(reponame, createTempFile(reponame, "dummyfile"), givenSqliteFilenameInfix);

    GridFSDBFile file = findSqLiteFile(reponame, givenSqliteFilenameInfix);
    assertThat(file, notNullValue());
    file.put("uploadDate", addHours(new Date(), -1));
    file.save();
    return (ObjectId) file.getId();
  }

  private GridFSDBFile findSqLiteFile(String reponame,
                                      String givenSqliteFilenameInfix) {
    return context.gridFs()
      .findOne(QueryBuilder.start("filename")
        .regex(compile("^" + reponame + "/repodata/" + givenSqliteFilenameInfix))
        .get());
  }

  private void assertRepoMdXml() {
    GridFSDBFile dbFile = context.gridFsService().findFileByPath(reponame + "/repodata/repomd.xml");
    assertThat(dbFile, notNullValue());
  }

  private void assertDbFile(String type) {
    GridFSDBFile dbFile = context.gridFsTemplate()
      .find(Query.query(GridFsCriteria.whereFilename().regex(reponame + "/repodata/.*" + type + ".*.sqlite.bz2")))
      .get(0);
    String sha256 = dbFile.getMetaData().get("sha256").toString();
    assertThat(dbFile.getFilename(), endsWith(type + "-" + sha256 + ".sqlite.bz2"));
    assertThat(dbFile.getMetaData().get(DatabaseStructure.MARKED_AS_DELETED_KEY), is(nullValue()));
  }

}
