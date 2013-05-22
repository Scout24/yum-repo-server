package de.is24.infrastructure.gridfs.http.rpm;

import de.is24.infrastructure.gridfs.http.domain.yum.YumPackage;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageChangeLog;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageDir;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFile;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFormat;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFormatEntry;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageRequirement;
import org.freecompany.redline.ReadableChannelWrapper;
import org.freecompany.redline.Scanner;
import org.freecompany.redline.header.Header;
import org.junit.BeforeClass;
import org.junit.Test;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.selectFirst;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.COMPLEX_RPM_ALL_FILES;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.COMPLEX_RPM_ARCH;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.COMPLEX_RPM_BUILD_HOST;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.COMPLEX_RPM_BUILD_TIME;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.COMPLEX_RPM_DESCRIPTION;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.COMPLEX_RPM_DIRS;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.COMPLEX_RPM_EPOCHE;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.COMPLEX_RPM_FILE_NAME;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.COMPLEX_RPM_GROUP;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.COMPLEX_RPM_HEADER_END;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.COMPLEX_RPM_HEADER_START;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.COMPLEX_RPM_INSTALLED_FILE_SIZE;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.COMPLEX_RPM_LICENSE;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.COMPLEX_RPM_LOCATION;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.COMPLEX_RPM_NAME;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.COMPLEX_RPM_RELEASE;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.COMPLEX_RPM_ROOT_FILES;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.COMPLEX_RPM_SOURCE_RPM;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.COMPLEX_RPM_SUMMARY;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.COMPLEX_RPM_URL;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.COMPLEX_RPM_VENDOR;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.COMPLEX_RPM_VERSION;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.SOURCE_RPM_ARCH;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.SOURCE_RPM_FILE_NAME;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.SOURCE_RPM_LOCATION;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.streamOf;
import static java.nio.channels.Channels.newChannel;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;


public class RpmHeaderToYumPackageConverterTest {
  private static RpmHeaderToYumPackageConverter rpmHeaderToYumPackageConverter;

  @BeforeClass
  public static void setUp() throws Exception {
    rpmHeaderToYumPackageConverter = headerConverter(COMPLEX_RPM_FILE_NAME);
  }

  @Test
  public void readPackageData() throws Exception {
    final YumPackage yumPackage = rpmHeaderToYumPackageConverter.convert();

    assertThat(yumPackage.getName(), equalTo(COMPLEX_RPM_NAME));
    assertThat(yumPackage.getArch(), equalTo(COMPLEX_RPM_ARCH));
    assertThat(yumPackage.getVersion().getVer(), equalTo(COMPLEX_RPM_VERSION));
    assertThat(yumPackage.getVersion().getRel(), equalTo(COMPLEX_RPM_RELEASE));
    assertThat(yumPackage.getSummary(), equalTo(COMPLEX_RPM_SUMMARY));
    assertThat(yumPackage.getDescription(), equalTo(COMPLEX_RPM_DESCRIPTION));
    assertThat(yumPackage.getUrl(), equalTo(COMPLEX_RPM_URL));
    assertThat(yumPackage.getTime().getBuild(), equalTo(COMPLEX_RPM_BUILD_TIME));
    assertThat(yumPackage.getSize().getInstalled(), equalTo(COMPLEX_RPM_INSTALLED_FILE_SIZE));
    assertThat(yumPackage.getSize().getArchive(), equalTo(0));
    assertThat(yumPackage.getLocation().getHref(), equalTo(COMPLEX_RPM_LOCATION));
  }

  @Test
  public void readSourceRpmPackageData() throws Exception {
    final YumPackage yumPackage = headerConverter(SOURCE_RPM_FILE_NAME).convert();
    assertThat(yumPackage.getArch(), equalTo(SOURCE_RPM_ARCH));
    assertThat(yumPackage.getLocation().getHref(), equalTo(SOURCE_RPM_LOCATION));
  }

  @Test
  public void readPackageFormatData() throws Exception {
    final YumPackageFormat packageFormat = rpmHeaderToYumPackageConverter.convert().getPackageFormat();
    assertThat(packageFormat.getLicense(), equalTo(COMPLEX_RPM_LICENSE));
    assertThat(packageFormat.getVendor(), equalTo(COMPLEX_RPM_VENDOR));
    assertThat(packageFormat.getGroup(), equalTo(COMPLEX_RPM_GROUP));
    assertThat(packageFormat.getBuildHost(), equalTo(COMPLEX_RPM_BUILD_HOST));
    assertThat(packageFormat.getSourceRpm(), equalTo(COMPLEX_RPM_SOURCE_RPM));
    assertThat(packageFormat.getHeaderStart(), is(COMPLEX_RPM_HEADER_START));
    assertThat(packageFormat.getHeaderEnd(), is(COMPLEX_RPM_HEADER_END));
  }

  @Test
  public void readPackageFormatEntryFlags() throws Exception {
    final Map<String, YumPackageFormatEntry> provides = makeAccessible(rpmHeaderToYumPackageConverter.convert()
      .getPackageFormat()
      .getProvides());

    assertThat(provides.get(COMPLEX_RPM_NAME).getFlags(), equalTo("EQ"));
  }

  @Test
  public void readPackageFormatEntryVersion() throws Exception {
    final Map<String, YumPackageFormatEntry> provides = makeAccessible(rpmHeaderToYumPackageConverter.convert()
      .getPackageFormat()
      .getProvides());

    assertThat(provides.get(COMPLEX_RPM_NAME).getVersion().getEpoch(), equalTo(COMPLEX_RPM_EPOCHE));
    assertThat(provides.get(COMPLEX_RPM_NAME).getVersion().getVer(), equalTo(COMPLEX_RPM_VERSION));
    assertThat(provides.get(COMPLEX_RPM_NAME).getVersion().getRel(), equalTo(COMPLEX_RPM_RELEASE));
  }

  @Test
  public void readPackageFormatProvides() throws Exception {
    final Map<String, YumPackageFormatEntry> provides = makeAccessible(rpmHeaderToYumPackageConverter.convert()
      .getPackageFormat()
      .getProvides());

    assertThat(provides.get("a_provides"), notNullValue());
    assertThat(provides.get("b_provides"), notNullValue());
    assertThat(provides.get("c_provides"), notNullValue());
    assertThat(provides.get(COMPLEX_RPM_NAME), notNullValue());
  }

  @Test
  public void readPackageFormatRequires() throws Exception {
    final Map<String, YumPackageRequirement> requires = makeAccessible(rpmHeaderToYumPackageConverter.convert()
      .getPackageFormat()
      .getRequires());

    assertThat(requires.get("a_require"), notNullValue());
    assertThat(requires.get("b_require"), notNullValue());
    assertThat(requires.get("c_require"), notNullValue());
    assertThat(requires.get("d_require"), notNullValue());
    assertThat(requires.get("pre_require"), notNullValue());
    assertThat(requires.get("pre_require").isPre(), is(true));
  }

  @Test
  public void readPackageFormatObsolete() throws Exception {
    final Map<String, YumPackageFormatEntry> obsoletes = makeAccessible(rpmHeaderToYumPackageConverter.convert()
      .getPackageFormat()
      .getObsoletes());

    assertThat(obsoletes.get("a_obsoletes"), notNullValue());
    assertThat(obsoletes.get("b_obsoletes"), notNullValue());
    assertThat(obsoletes.get("c_obsoletes"), notNullValue());
  }

  @Test
  public void readPackageChangeLog() throws Exception {
    List<YumPackageChangeLog> changeLogs = rpmHeaderToYumPackageConverter.convert().getChangeLogs();

    assertThat(changeLogs.get(0).getAuthor(), equalTo("- second"));
    assertThat(changeLogs.get(0).getDate(), equalTo(1359806400));
    assertThat(changeLogs.get(0).getMessage(), equalTo("- added second part I\n- added second part II"));
    assertThat(changeLogs.get(1).getAuthor(), equalTo("- first"));
    assertThat(changeLogs.get(1).getDate(), equalTo(1359720000));
    assertThat(changeLogs.get(1).getMessage(), equalTo("- added first"));
  }

  @Test
  public void readAllPackageFiles() throws Exception {
    int count = 0;
    for (YumPackageDir dir : rpmHeaderToYumPackageConverter.convert().getPackageDirs()) {
      count += dir.getFiles().size();
    }

    assertThat(count, equalTo(COMPLEX_RPM_ALL_FILES.length));
  }

  @Test
  public void readAllPackageDirs() throws Exception {
    YumPackageDir[] yumPackageDirs = rpmHeaderToYumPackageConverter.convert().getPackageDirs();

    assertThat(yumPackageDirs.length, equalTo(COMPLEX_RPM_DIRS.length));
    for (YumPackageDir dir : yumPackageDirs) {
      assertThat(COMPLEX_RPM_DIRS, hasItemInArray(dir.getName()));
    }
  }

  @Test
  public void readFilesForRootDir() throws Exception {
    YumPackageDir rootDir = selectFirst(rpmHeaderToYumPackageConverter.convert().getPackageDirs(),
      having(on(YumPackageDir.class).getName(), equalTo("/")));

    assertThat(rootDir.getFiles().size(), equalTo(COMPLEX_RPM_ROOT_FILES.length));
    for (YumPackageFile file : COMPLEX_RPM_ROOT_FILES) {
      assertThat(rootDir.getFiles(), hasItem(file));
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends YumPackageFormatEntry> Map<String, T> makeAccessible(final List<T> entries) {
    final Map<String, T> accessibleEntries = new HashMap<>();
    for (YumPackageFormatEntry entry : entries) {
      accessibleEntries.put(entry.getName(), (T) entry);
    }

    return accessibleEntries;
  }

  private static RpmHeaderToYumPackageConverter headerConverter(String fileName) throws Exception {
    return new RpmHeaderToYumPackageConverter(new RpmHeaderWrapper(readHeader(streamOf(fileName))));
  }


  private static Header readHeader(InputStream inputStream) throws Exception {
    return new Scanner().run(new ReadableChannelWrapper(newChannel(inputStream))).getHeader();
  }
}
