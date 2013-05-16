package de.is24.infrastructure.gridfs.http.metadata.generation;

import de.is24.infrastructure.gridfs.http.domain.YumEntry;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackage;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageChangeLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.reverse;

public class OtherDbGenerator extends DbGenerator {

  public OtherDbGenerator() {
    super("other");
  }

  @Override
  protected Map<String, PreparedStatement> createPreparedStatements(Connection connection) throws SQLException {
    Map<String, PreparedStatement> statements = new HashMap<>();
    statements.put(PACKAGES, connection.prepareStatement("insert into packages values (?, ?)"));
    statements.put(CHANGELOG, connection.prepareStatement("insert into changelog values (?, ?, ?, ?)"));
    return statements;
  }

  @Override
  protected void writeEntry(Map<String, PreparedStatement> preparedStatements, int pkgKey, YumEntry entry) throws SQLException {
    YumPackage yumPackage = entry.getYumPackage();
      writePackageTable(preparedStatements.get(PACKAGES), pkgKey, yumPackage);
      writeChangeLogTable(preparedStatements.get(CHANGELOG), pkgKey, yumPackage);
  }

  private void writeChangeLogTable(final PreparedStatement ps, final int index, final YumPackage yumPackage) throws SQLException {
    reverse(yumPackage.getChangeLogs());
    for (YumPackageChangeLog changeLog : yumPackage.getChangeLogs()) {
      int c = 1;
      ps.setInt(c++, index);
      ps.setString(c++, changeLog.getAuthor());
      ps.setInt(c++, changeLog.getDate());
      ps.setString(c++, changeLog.getMessage());
      ps.addBatch();
    }

    if (!yumPackage.getChangeLogs().isEmpty()) {
      ps.executeBatch();
    }
  }

  private void writePackageTable(final PreparedStatement ps, final int index, final YumPackage yumPackage) throws SQLException {
    ps.setInt(1, index);
    ps.setString(2, yumPackage.getChecksum().getChecksum());
    ps.executeUpdate();
  }
}
