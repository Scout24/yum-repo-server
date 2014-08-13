package de.is24.infrastructure.gridfs.http.metadata.generation;

import de.is24.infrastructure.gridfs.http.domain.YumEntry;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackage;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageDir;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.join;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.EMPTY;


public class FileListsGenerator extends DbGenerator {
  private static final String FILE_NAMES_SEPARATOR = "/";

  public FileListsGenerator() {
    super("filelists");
  }

  @Override
  protected Map<String, PreparedStatement> createPreparedStatements(Connection connection) throws SQLException {
    Map<String, PreparedStatement> statements = new HashMap<>();
    statements.put(PACKAGES, connection.prepareStatement("insert into packages values (?, ?)"));
    statements.put(FILELIST, connection.prepareStatement("insert into filelist values (?, ?, ?, ?)"));
    return statements;
  }

  @Override
  protected void writeEntry(Map<String, PreparedStatement> preparedStatements, int pkgKey, YumEntry entry)
                     throws SQLException {
    YumPackage yumPackage = entry.getYumPackage();
    writePackageTable(preparedStatements.get(PACKAGES), pkgKey, yumPackage);
    writeFilelistsTable(preparedStatements.get(FILELIST), pkgKey, yumPackage);
  }

  private void writeFilelistsTable(final PreparedStatement ps, final int index, final YumPackage yumPackage)
                            throws SQLException {
    for (YumPackageDir dir : yumPackage.getPackageDirs()) {
      List<YumPackageFile> filesForDirectory = getSortedListOfFiles(dir);
      int c = 0;
      ps.setInt(++c, index);
      ps.setString(++c, dir.getName());
      ps.setString(++c, join(FILE_NAMES_SEPARATOR, getFilenames(filesForDirectory)));
      ps.setString(++c, join(EMPTY, extractFileTypeChars(filesForDirectory)));
      ps.addBatch();
    }

    if (yumPackage.getPackageDirs().length > 0) {
      ps.executeBatch();
    }
  }

  private void writePackageTable(final PreparedStatement ps, final int index, final YumPackage yumPackage)
                          throws SQLException {
    ps.setInt(1, index);
    ps.setString(2, yumPackage.getChecksum().getChecksum());
    ps.executeUpdate();
  }

  private List<String> extractFileTypeChars(final List<YumPackageFile> files) {
    return files.stream().map(file -> file.getType().getTypeChar()).collect(toList());
  }

  private List<String> getFilenames(final List<YumPackageFile> filesForDirectory) {
    return filesForDirectory.stream().map(YumPackageFile::getName).collect(toList());
  }

  private List<YumPackageFile> getSortedListOfFiles(final YumPackageDir dir) {
    return dir.getFiles().stream().sorted((file1, file2) ->
        file1.getType().getFilelistsOrder().compareTo(file2.getType().getFilelistsOrder())).collect(toList());
  }
}
