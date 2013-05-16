package de.is24.infrastructure.gridfs.http.metadata.generation;

import de.is24.infrastructure.gridfs.http.domain.YumEntry;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackage;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageDir;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFile;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFileTypeComparator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static ch.lambdaj.Lambda.extract;
import static ch.lambdaj.Lambda.join;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.sort;
import static org.apache.commons.lang.StringUtils.EMPTY;


public class FileListsGenerator extends DbGenerator {
  private static final YumPackageFileTypeComparator YUM_PACKAGE_FILE_TYPE_COMPARATOR =
    new YumPackageFileTypeComparator();
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
      int c = 1;
      ps.setInt(c++, index);
      ps.setString(c++, dir.getName());
      ps.setString(c++, join(getFilenames(filesForDirectory), FILE_NAMES_SEPARATOR));
      ps.setString(c++, join(extractFileTypeChars(filesForDirectory), EMPTY));
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
    List<String> fileTypeChars = new ArrayList<String>();
    for (YumPackageFile file : files) {
      fileTypeChars.add(file.getType().getTypeChar());
    }
    return fileTypeChars;
  }

  private List<String> getFilenames(final List<YumPackageFile> filesForDirectory) {
    return extract(filesForDirectory, on(YumPackageFile.class).getName());
  }

  private List<YumPackageFile> getSortedListOfFiles(final YumPackageDir dir) {
    return sort(dir.getFiles(), on(YumPackageFile.class).getType(), YUM_PACKAGE_FILE_TYPE_COMPARATOR);
  }
}
