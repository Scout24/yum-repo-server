package de.is24.infrastructure.gridfs.http.metadata.generation;

import de.is24.infrastructure.gridfs.http.category.LocalExecutionOnly;
import de.is24.infrastructure.gridfs.http.mongo.IntegrationTestContext;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.uniqueRepoName;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.COMPLEX_RPM_FILE_NAME;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.COMPLEX_RPM_FILE_NAMES_WITHOUT_ROOT_DIR;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.streamOf;
import static java.io.File.createTempFile;
import static java.sql.DriverManager.getConnection;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@Category(LocalExecutionOnly.class)
public class MetadataGeneratorIT {

  private static final String[] FILELISTS_TABLE_NAMES = {"db_info", "filelist", "packages"};
  private static final String[] FILELISTS_INDEX_NAMES = {"dirnames", "pkgId", "keyfile"};

  private static final Map<String, Integer[]> PRIMARY_IGNORED_COLUMNS = new HashMap<>();
  private static final Map<String, Integer[]> OTHER_IGNORED_COLUMNS = new HashMap<>();

  private static final String VALID_PRIMARY_SQLITE = "/repodata/valid.headertoyumpackage.noarch.rpm/primary.sqlite";
  private static final String VALID_OTHER_SQLITE = "/repodata/valid.headertoyumpackage.noarch.rpm/other.sqlite";
  private static final String FILELIST_SEPARATOR = "/";
  private static final String SPECIAL_ROOT_DIR_SEQUENCE = "///";
  private static final String[] TABLE_TYPE = {"table"};
  private static final String[] INDEX_TYPE = {"index"};

  @ClassRule
  public static IntegrationTestContext context = new IntegrationTestContext();

  private DbGenerator generator;
  private File dbFile;

  @BeforeClass
  public static void beforeClass() {
    PRIMARY_IGNORED_COLUMNS.put("db_info", new Integer[]{2});
    PRIMARY_IGNORED_COLUMNS.put("packages", new Integer[]{11, 23});
    PRIMARY_IGNORED_COLUMNS.put("obsoletes", new Integer[]{1});
    OTHER_IGNORED_COLUMNS.put("db_info", new Integer[]{2});
  }

  @Before
  public void setUp() throws Exception {
    dbFile = createTempFile("db_file", "sqlite");
  }

  @After
  public void tearDown() {
    dbFile.delete();
  }

  @Test
  public void createPrimaryDb() throws Exception {
    String repoName = uniqueRepoName();
    context.gridFsService().storeRpm(repoName, streamOf(COMPLEX_RPM_FILE_NAME));
    generator = new PrimaryDbGenerator();
    generator.createDb(dbFile, context.yumEntriesRepository().findByRepo(repoName));
    assertDb(dbFile, openSqLiteFile(VALID_PRIMARY_SQLITE), PRIMARY_IGNORED_COLUMNS);
  }

  @Test
  public void createFilelistsDB() throws Exception {
    String repoName = uniqueRepoName();
    context.gridFsService().storeRpm(repoName, streamOf(COMPLEX_RPM_FILE_NAME));
    generator = new FileListsGenerator();
    generator.createDb(dbFile, context.yumEntriesRepository().findByRepo(repoName));
    assertDb(dbFile, FILELISTS_TABLE_NAMES, FILELISTS_INDEX_NAMES);
  }

  @Test
  public void filelistsIgnoredColumn() throws Exception {
    String repoName = uniqueRepoName();
    context.gridFsService().storeRpm(repoName, streamOf(COMPLEX_RPM_FILE_NAME));
    generator = new FileListsGenerator();
    generator.createDb(dbFile, context.yumEntriesRepository().findByRepo(repoName));
    String fileNamesFromDb = readFileNameFromDb();

    assertThat(fileNamesFromDb, containsString(SPECIAL_ROOT_DIR_SEQUENCE));
    assertThat(asList(removeRootDirSequence(fileNamesFromDb).split("/")), is(COMPLEX_RPM_FILE_NAMES_WITHOUT_ROOT_DIR));
  }

  @Test
  public void createOtherDb() throws Exception {
    String reponame = uniqueRepoName();
    context.gridFsService().storeRpm(reponame, streamOf(COMPLEX_RPM_FILE_NAME));
    generator = new OtherDbGenerator();
    generator.createDb(dbFile, context.yumEntriesRepository().findByRepo(reponame));
    assertDb(dbFile, openSqLiteFile(VALID_OTHER_SQLITE), OTHER_IGNORED_COLUMNS);
  }

  private String removeRootDirSequence(final String fileNamesFromDb) {
    return fileNamesFromDb.replaceAll(SPECIAL_ROOT_DIR_SEQUENCE, FILELIST_SEPARATOR);
  }

  private String readFileNameFromDb() throws SQLException {
    try (Connection connection = getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath())) {
      try (Statement statement = connection.createStatement()) {
        String query = "select filenames from filelist where dirname = '/'";
        ResultSet resultSet = statement.executeQuery(query);
        if (resultSet.isAfterLast()) {
          throw new IllegalArgumentException("Didn't find a row for query: " + query);
        }
        return resultSet.getString(1);
      }
    }
  }

  private File openSqLiteFile(String path) throws Exception {
    return new File(getClass().getResource(path).toURI());
  }

  private void assertDb(File fileToTest, File expectedFile, Map<String, Integer[]> ignoredColumns) throws Exception {
    try (Connection connectionToTest = getConnection("jdbc:sqlite:" + fileToTest.getAbsolutePath())) {
      try (Connection expectedConnection = getConnection("jdbc:sqlite:" + expectedFile.getAbsolutePath())) {
        assertThat(getTypeNames(connectionToTest, TABLE_TYPE), is(getTypeNames(expectedConnection, TABLE_TYPE)));
        assertThat(getTypeNames(connectionToTest, INDEX_TYPE), is(getTypeNames(expectedConnection, INDEX_TYPE)));
        for (String tableName : getTypeNames(connectionToTest, TABLE_TYPE)) {
          assertTable(connectionToTest, tableName, expectedConnection, ignoredColumns.get(tableName));
        }
      }
    }
  }

  private void assertTable(Connection connectionToTest, String tableName, Connection expectedConnection, Integer[] ignoredColumns) throws Exception {
    String tableQuery = buildOrderedTableQuery(tableName, connectionToTest);
    try (ResultSet resultSetToTest = connectionToTest.createStatement().executeQuery(tableQuery)) {
      try (ResultSet expectedResultSet = expectedConnection.createStatement().executeQuery(tableQuery)) {
        assertThat("EOF for should be the same for table: " + tableName, resultSetToTest.isAfterLast(), is(expectedResultSet.isAfterLast()));

        if (!resultSetToTest.isAfterLast()) {
          assertRecords(resultSetToTest, expectedResultSet, ignoredColumns);
        }
      }
    }
  }

  private String buildOrderedTableQuery(String tableName, Connection connection) throws SQLException {
    String columnName = findNameColumn(connection, tableName);
    if (columnName != null) {
      return "select * from " + tableName + " order by " + columnName;
    }

    return "select * from " + tableName;
  }

  private String findNameColumn(Connection connection, String tableName) throws SQLException {
    ResultSet resultSet = connection.getMetaData().getIndexInfo(null, null, tableName, false, false);
    while (resultSet.next()) {
      String indexName = resultSet.getString("INDEX_NAME");
      if (indexName.contains("name")) {
        return resultSet.getString("COLUMN_NAME");
      }
    }

    return null;
  }

  private void assertRecords(ResultSet resultSetToTest, ResultSet expectedResultSet, Integer[] ignoredColumns) throws SQLException {
    boolean nextToTest = resultSetToTest.next();
    boolean expectedNext = expectedResultSet.next();
    Set<Integer> ignored = ignoredColumns != null ? new HashSet<>(asList(ignoredColumns)) : new HashSet<Integer>();
    while (nextToTest && expectedNext) {
      for (int column = 1; column <= resultSetToTest.getMetaData().getColumnCount(); column++) {
        if (!ignored.contains(column)) {
          assertThat(resultSetToTest.getObject(column), is(expectedResultSet.getObject(column)));
        }
      }
      nextToTest = resultSetToTest.next();
      expectedNext = expectedResultSet.next();
    }

    assertThat(nextToTest, is(expectedNext));
  }

  private void assertDb(File dbFile, String[] expectedTableNames, String[] expectedIndexNames) throws SQLException {
    try (Connection connection = getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath())) {
      assertThat(getTypeNames(connection, TABLE_TYPE), is(sortedSet(expectedTableNames)));
      assertThat(getTypeNames(connection, INDEX_TYPE), is(sortedSet(expectedIndexNames)));
    }
  }

  private SortedSet<String> sortedSet(String[] strings) {
    return new TreeSet<>(asList(strings));
  }

  private SortedSet<String> getTypeNames(Connection connection, String[] types) throws SQLException {
    SortedSet<String> dbTableNames = new TreeSet<>();
    ResultSet resultSet = connection.getMetaData().getTables(null, null, "%", types);
    while (resultSet.next()) {
      dbTableNames.add(resultSet.getString(3));
    }
    return dbTableNames;
  }

}
