package de.is24.infrastructure.gridfs.http.metadata.generation;

import de.is24.infrastructure.gridfs.http.domain.YumEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static java.sql.DriverManager.getConnection;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.readLines;
import static org.apache.commons.lang.StringUtils.defaultIfBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

public abstract class DbGenerator {

  private static final Logger LOG = LoggerFactory.getLogger(DbGenerator.class);
  public static final int DB_VERSION = 10;
  protected static final String SQL_DIR = "/sql/";
  protected static final String PACKAGES = "packages";
  protected static final String CHANGELOG = "changelog";
  protected static final String FILES = "files";
  protected static final String FILELIST = "filelist";

  private final String name;

  protected DbGenerator(String name) {
    this.name = name;
    initJdbC();
  }

  public void createDb(File dbFile, List<YumEntry> entries) throws Exception {
    dbFile.delete();
    try (Connection connection = getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath())) {
      initSchema(connection, getName() + ".sql");
      Map<String, PreparedStatement> preparedStatements = createPreparedStatements(connection);
      try {
        int pkgKey = 1;
        for (YumEntry entry : entries) {
          writeEntry(preparedStatements, pkgKey, entry);
          pkgKey++;
        }
      } finally {
        close(preparedStatements);
      }
    } catch (Exception e) {
      LOG.error("Could not generate metadata for repository: {}", name, e);
      throw e;
    }
  }

  protected abstract void writeEntry(Map<String, PreparedStatement> preparedStatements, int pkgKey, YumEntry entry) throws SQLException;

  protected abstract Map<String, PreparedStatement> createPreparedStatements(Connection connection) throws SQLException;

  public String getName() {
    return name;
  }

  protected void initSchema(Connection connection, String schemaFile) throws IOException, SQLException {
    try (Statement statement = connection.createStatement()) {
      for (String command : readCommands(schemaFile)) {
        statement.executeUpdate(command);
      }
      insertVersion(statement);
    }
  }

  protected List<String> readCommands(String filename) throws IOException {
    InputStream inputStream = getClass().getResourceAsStream(SQL_DIR + filename);
    if (inputStream == null) {
      throw new IllegalStateException("Could not find prepared sql: " + filename);
    }
    try {
      return readLines(inputStream);
    } finally {
      closeQuietly(inputStream);
    }
  }

  protected void insertVersion(Statement statement) throws SQLException {
    statement.executeUpdate("insert into db_info values(" + DB_VERSION + ", 'direct_create')");
  }

  protected void initJdbC() {
    try {
      Class.forName("org.sqlite.JDBC");
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("SQLite JDBC driver not found", e);
    }
  }

  protected void close(Map<String, PreparedStatement> preparedStatements) throws SQLException {
    for (Map.Entry<String, PreparedStatement> entry : preparedStatements.entrySet()) {
      entry.getValue().close();
    }
  }

  protected static String emptyIfBlank(String str) {
    return defaultIfBlank(str, "");
  }

  protected static String nullIfBlank(String str) {
    return defaultIfBlank(str, null);
  }

  protected static String trim(String str) {
    if (isNotBlank(str)) {
      return str.trim();
    }

    return null;
  }
}
