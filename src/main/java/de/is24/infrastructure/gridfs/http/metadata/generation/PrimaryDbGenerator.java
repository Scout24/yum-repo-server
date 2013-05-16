package de.is24.infrastructure.gridfs.http.metadata.generation;

import de.is24.infrastructure.gridfs.http.domain.YumEntry;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackage;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageDir;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFile;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFormatEntry;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageRequirement;
import org.hamcrest.Matcher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.select;
import static de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFileType.DIR;
import static de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFileType.FILE;
import static de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFileType.GHOST;
import static org.apache.commons.lang.StringUtils.defaultIfBlank;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;


public class PrimaryDbGenerator extends DbGenerator {
  public static final String OBSOLETES = "obsoletes";
  public static final String CONFLICTS = "conflicts";
  public static final String PROVIDES = "provides";
  public static final String REQUIRES = "requires";

  public PrimaryDbGenerator() {
    super("primary");
  }

  @Override
  protected Map<String, PreparedStatement> createPreparedStatements(Connection connection) throws SQLException {
    Map<String, PreparedStatement> preparedStatements = new HashMap<>();
    preparedStatements.put(PACKAGES,
      connection.prepareStatement(
        "insert into packages values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"));
    preparedStatements.put(OBSOLETES, connection.prepareStatement("insert into obsoletes values (?, ?, ?, ?, ?, ?)"));
    preparedStatements.put(CONFLICTS, connection.prepareStatement("insert into conflicts values (?, ?, ?, ?, ?, ?)"));
    preparedStatements.put(PROVIDES, connection.prepareStatement("insert into provides values (?, ?, ?, ?, ?, ?)"));
    preparedStatements.put(REQUIRES, connection.prepareStatement("insert into requires values (?, ?, ?, ?, ?, ?, ?)"));
    preparedStatements.put(FILES, connection.prepareStatement("insert into files values (?, ?, ?)"));
    return preparedStatements;
  }

  @Override
  protected void writeEntry(Map<String, PreparedStatement> ps, int pkgKey, YumEntry entry) throws SQLException {
    YumPackage p = entry.getYumPackage();
    writePrimaryPackage(ps.get(PACKAGES), pkgKey, p);
    writeDependency(ps.get(OBSOLETES), pkgKey, p.getPackageFormat().getObsoletes());
    writeDependency(ps.get(PROVIDES), pkgKey, p.getPackageFormat().getProvides());
    writeDependency(ps.get(CONFLICTS), pkgKey, p.getPackageFormat().getConflicts());
    writeRequires(ps.get(REQUIRES), pkgKey, filterRequires(p.getPackageFormat().getRequires()));

    PreparedStatement filesStatement = ps.get(FILES);
    writePrimaryFiles(filesStatement, p, pkgKey, new PrimaryFileNameMatcher(FILE));
    writePrimaryFiles(filesStatement, p, pkgKey, new PrimaryDirNameMatcher(DIR));
    writePrimaryFiles(filesStatement, p, pkgKey, new PrimaryFileNameMatcher(GHOST));
  }

  private void writePrimaryPackage(PreparedStatement ps, int pkgKey, YumPackage p) throws SQLException {
    int c = 1;
    ps.setInt(c++, pkgKey);
    ps.setString(c++, p.getChecksum().getChecksum());
    ps.setString(c++, p.getName());
    ps.setString(c++, p.getArch());
    ps.setString(c++, p.getVersion().getVer());
    ps.setInt(c++, p.getVersion().getEpoch());
    ps.setString(c++, p.getVersion().getRel());
    ps.setString(c++, trim(p.getSummary()));
    ps.setString(c++, trim(p.getDescription()));
    ps.setString(c++, defaultIfBlank(p.getUrl(), null));
    ps.setInt(c++, p.getTime().getFile());
    ps.setInt(c++, p.getTime().getBuild());
    ps.setString(c++, emptyIfBlank(p.getPackageFormat().getLicense()));
    ps.setString(c++, emptyIfBlank(p.getPackageFormat().getVendor()));
    ps.setString(c++, emptyIfBlank(p.getPackageFormat().getGroup()));
    ps.setString(c++, emptyIfBlank(p.getPackageFormat().getBuildHost()));
    ps.setString(c++, emptyIfBlank(p.getPackageFormat().getSourceRpm()));
    ps.setInt(c++, p.getPackageFormat().getHeaderStart());
    ps.setInt(c++, p.getPackageFormat().getHeaderEnd());
    ps.setString(c++, nullIfBlank(p.getPackager()));
    ps.setInt(c++, p.getSize().getPackaged());
    ps.setInt(c++, p.getSize().getInstalled());
    ps.setInt(c++, p.getSize().getArchive());
    ps.setString(c++, p.getLocation().getHref());
    ps.setString(c++, null);
    ps.setString(c, p.getChecksum().getType());
    ps.executeUpdate();
  }

  private void writeDependency(PreparedStatement ps, int pkgKey, List<YumPackageFormatEntry> dependencies)
                        throws SQLException {
    for (YumPackageFormatEntry dependency : dependencies) {
      int c = fillStatementForYumPackageFormatEntry(ps, dependency, 1);
      ps.setInt(c, pkgKey);
      ps.addBatch();
    }
    if (!dependencies.isEmpty()) {
      ps.executeBatch();
    }
  }

  private static int fillStatementForYumPackageFormatEntry(final PreparedStatement ps,
                                                           final YumPackageFormatEntry dependency, int counter)
                                                    throws SQLException {
    ps.setString(counter++, dependency.getName());
    if (dependency.getFlags() == null) {
      ps.setString(counter++, null);
      ps.setString(counter++, null);
      ps.setString(counter++, null);
      ps.setString(counter++, null);
    } else {
      ps.setString(counter++, dependency.getFlags());
      ps.setInt(counter++, dependency.getVersion().getEpoch());
      ps.setString(counter++, nullIfBlank(dependency.getVersion().getVer()));
      ps.setString(counter++, nullIfBlank(dependency.getVersion().getRel()));
    }
    return counter;
  }

  private void writeRequires(PreparedStatement ps, int pkgKey, List<YumPackageRequirement> dependencies)
                      throws SQLException {
    for (YumPackageRequirement dependency : dependencies) {
      int c = fillStatementForYumPackageFormatEntry(ps, dependency, 1);
      ps.setInt(c++, pkgKey);
      ps.setString(c, dependency.isPre().toString().toUpperCase());
      ps.addBatch();
    }
    if (!dependencies.isEmpty()) {
      ps.executeBatch();
    }
  }

  private List<YumPackageRequirement> filterRequires(List<YumPackageRequirement> requires) {
    return select(requires, not(having(on(YumPackageRequirement.class).getName(), startsWith("rpmlib("))));
  }

  private void writePrimaryFiles(PreparedStatement ps, YumPackage yumPackage, int pkgKey,
                                 Matcher<YumPackageFile> matcher) throws SQLException {
    int counter = 0;
    for (YumPackageDir dir : yumPackage.getPackageDirs()) {
      for (YumPackageFile file : select(dir.getFiles(), matcher)) {
        ps.setString(1, file.getDir() + file.getName());
        ps.setString(2, file.getType().toString().toLowerCase());
        ps.setInt(3, pkgKey);
        ps.addBatch();
        counter++;
      }
    }
    if (counter > 0) {
      ps.executeBatch();
    }
  }
}
