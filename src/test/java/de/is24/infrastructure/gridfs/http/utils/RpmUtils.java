package de.is24.infrastructure.gridfs.http.utils;

import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFile;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFileType.*;
import static java.util.Arrays.asList;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.apache.commons.lang.ArrayUtils.addAll;

public final class RpmUtils {

  private RpmUtils() {
  }

  public static final String SOURCE_RPM_FILE_NAME = "valid.src.rpm";
  public static final String SOURCE_RPM_ARCH = "src";
  public static final String SOURCE_RPM_LOCATION = "src/yum-repo-client-1.1-273.src.rpm";

  public static final String COMPLEX_RPM_FILE_NAME = "valid.headertoyumpackage.noarch.rpm";
  public static final String COMPLEX_RPM_NAME = "is24-complex-test";
  public static final String COMPLEX_RPM_ARCH = "noarch";

  public static final int COMPLEX_RPM_EPOCHE = 0;
  public static final String COMPLEX_RPM_VERSION = "1.1.0";
  public static final String COMPLEX_RPM_RELEASE = "2.410395";
  public static final String COMPLEX_RPM_SUMMARY = "Test Rpm for new Repo Server";
  public static final String COMPLEX_RPM_URL = "http://www.immobilienscout24.de/";
  public static final int COMPLEX_RPM_BUILD_TIME = 1363882791;
  public static final int COMPLEX_RPM_INSTALLED_FILE_SIZE = 100000;
  public static final int COMPLEX_RPM_HEADER_START = 280;
  public static final int COMPLEX_RPM_HEADER_END = 3356;
  public static final String COMPLEX_RPM_LICENSE = "LGPL";
  public static final String COMPLEX_RPM_VENDOR = "IS24 Cld TF";
  public static final String COMPLEX_RPM_GROUP = "is24-tf";
  public static final String COMPLEX_RPM_BUILD_HOST = "tuvbui04.dev.is24.loc";
  public static final String COMPLEX_RPM_DESCRIPTION = "This RPM is used to test our RPM header parser.";
  public static final String COMPLEX_RPM_LOCATION = COMPLEX_RPM_ARCH + "/" + COMPLEX_RPM_NAME + "-" + COMPLEX_RPM_VERSION + "-" + COMPLEX_RPM_RELEASE + "." + COMPLEX_RPM_ARCH + ".rpm";
  public static final String COMPLEX_RPM_SOURCE_RPM = COMPLEX_RPM_NAME + "-" + COMPLEX_RPM_VERSION + "-" + COMPLEX_RPM_RELEASE + ".src.rpm";

  public static final File RPM_FILE = new File("src/test/resources/rpms/valid.noarch.rpm");
  public static final String RPM_FILE_LOCATION = "noarch/test-artifact-1.2-1.noarch.rpm";
  public static final int RPM_FILE_SIZE = 1364;

  public static final String[] COMPLEX_RPM_DIRS = {"/", "/etc", "/bin", "/data.is24"};
  public static final List<String> COMPLEX_RPM_FILE_NAMES_WITHOUT_ROOT_DIR = asList("z_ghost.txt", "bin", "data.is24", "empty_dir", "etc", "a_file.txt", "b_file.rpm", "c_file.zip", "d.file.exe");

  public static final YumPackageFile[] COMPLEX_RPM_ROOT_FILES = {
      new YumPackageFile(GHOST, "z_ghost.txt", "/"),
      new YumPackageFile(DIR, "etc", "/"),
      new YumPackageFile(DIR, "empty_dir", "/"),
      new YumPackageFile(DIR, "bin", "/"),
      new YumPackageFile(DIR, "", "/"),
      new YumPackageFile(FILE, "d.file.exe", "/"),
      new YumPackageFile(FILE, "c_file.zip", "/"),
      new YumPackageFile(FILE, "b_file.rpm", "/"),
      new YumPackageFile(FILE, "b_file.rpm", "/"),
      new YumPackageFile(FILE, "a_file.txt", "/")
  };

  public static final YumPackageFile[] COMPLEX_RPM_OTHER_FILES = {
      new YumPackageFile(FILE, "app", "/bin"),
      new YumPackageFile(FILE, "READMD.md", "/data.is24"),
      new YumPackageFile(FILE, "config", "/etc"),
  };

  public static final YumPackageFile[] COMPLEX_RPM_ALL_FILES = (YumPackageFile[]) addAll(COMPLEX_RPM_ROOT_FILES, COMPLEX_RPM_OTHER_FILES);

  public static InputStream streamOf(String fileName) throws IOException {
    return RpmUtils.class.getResourceAsStream("/rpms/" + fileName);
  }
}
