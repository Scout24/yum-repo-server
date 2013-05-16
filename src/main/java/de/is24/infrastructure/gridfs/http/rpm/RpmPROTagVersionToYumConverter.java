package de.is24.infrastructure.gridfs.http.rpm;

import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageVersion;
import java.util.regex.Pattern;
import static java.lang.Integer.parseInt;


public final class RpmPROTagVersionToYumConverter {
  private static final String EPOCH_SEPARATOR = ":";
  private static final String VERSION_SEPARATOR = "-";
  private static final Pattern EPOCH_PATTERN = Pattern.compile("\\d+");
  private static final String EMPTY = "";

  private RpmPROTagVersionToYumConverter() {
  }

  public static YumPackageVersion convert(final String version) {
    final YumPackageVersion yumPackageVersion = new YumPackageVersion();
    if (null != version) {
      yumPackageVersion.setEpoch(getEpoch(version));
      yumPackageVersion.setVer(getVersion(version));
      yumPackageVersion.setRel(getRelease(version));
    }

    return yumPackageVersion;
  }

  private static String getRelease(final String version) {
    final int versionIndex = version.indexOf(VERSION_SEPARATOR);
    return (versionIndex != -1) ? version.substring(versionIndex + 1) : EMPTY;
  }

  private static String getVersion(final String version) {
    final int epochIndex = version.indexOf(EPOCH_SEPARATOR);
    final int versionIndex = version.indexOf(VERSION_SEPARATOR);

    final int versionStart = (epochIndex != -1) ? (epochIndex + 1) : 0;
    final int versionEnd = (versionIndex != -1) ? versionIndex : version.length();

    return version.substring(versionStart, versionEnd);
  }

  private static int getEpoch(final String version) {
    final String[] parts = version.split(EPOCH_SEPARATOR);

    return (parts.length != 2) ? 0 : getEpochValue(parts[0]);
  }

  private static int getEpochValue(final String value) {
    return EPOCH_PATTERN.matcher(value).matches() ? parseInt(value) : 0;
  }
}
