package de.is24.infrastructure.gridfs.http.rpm;

import de.is24.infrastructure.gridfs.http.domain.yum.YumPackage;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFormat;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageLocation;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageSize;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageTime;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageVersion;
import de.is24.infrastructure.gridfs.http.exception.InvalidRpmHeaderException;
import org.freecompany.redline.header.Header;
import static org.apache.commons.lang.StringUtils.isBlank;


public class RpmHeaderToYumPackageConverter {
  public static final String NO_SOURCE_RPM_VALUE = "(none)";
  private final RpmHeaderWrapper rpmHeaderWrapper;
  private final RpmToYumPROConverter rpmToYumPROConverter;
  private final RpmToYumChangeLogConverter rpmToYumChangeLogConverter;

  public RpmHeaderToYumPackageConverter(RpmHeaderWrapper rpmHeaderWrapper) {
    this.rpmHeaderWrapper = rpmHeaderWrapper;
    this.rpmToYumPROConverter = new RpmToYumPROConverter(rpmHeaderWrapper);
    this.rpmToYumChangeLogConverter = new RpmToYumChangeLogConverter(rpmHeaderWrapper);
  }

  public YumPackage convert() throws InvalidRpmHeaderException {
    final YumPackage yumPackage = readYumPackage();
    yumPackage.setVersion(readRpmVersion());
    yumPackage.setTime(readRpmTime());
    yumPackage.setSize(readRpmSize());
    yumPackage.setLocation(createYumPackageLocation(yumPackage));
    yumPackage.setPackageFormat(readPackageFormat());
    yumPackage.setChangeLogs(rpmToYumChangeLogConverter.convert());
    yumPackage.setPackageDirs(new RpmToYumFilesConverter(rpmHeaderWrapper).readFiles());
    return yumPackage;
  }

  private YumPackageLocation createYumPackageLocation(final YumPackage yumPackage) {
    YumPackageLocation yumPackageLocation = new YumPackageLocation();
    yumPackageLocation.setHref(createFilename(yumPackage));
    return yumPackageLocation;
  }

  private YumPackage readYumPackage() throws InvalidRpmHeaderException {
    final YumPackage yumPackage = new YumPackage();
    yumPackage.setName(rpmHeaderWrapper.readString(Header.HeaderTag.NAME, true));

    String arch = rpmHeaderWrapper.readString(Header.HeaderTag.ARCH, true);
    String sourceRpm = rpmHeaderWrapper.readString(Header.HeaderTag.SOURCERPM, false);
    yumPackage.setArch(calculateArch(arch, sourceRpm));
    yumPackage.setSummary(rpmHeaderWrapper.readString(Header.HeaderTag.SUMMARY, false));
    yumPackage.setDescription(rpmHeaderWrapper.readString(Header.HeaderTag.DESCRIPTION, false));
    yumPackage.setPackager(rpmHeaderWrapper.readString(Header.HeaderTag.PACKAGER, false));
    yumPackage.setUrl(rpmHeaderWrapper.readString(Header.HeaderTag.URL, false));
    return yumPackage;
  }

  private YumPackageFormat readPackageFormat() throws InvalidRpmHeaderException {
    final YumPackageFormat packageFormat = new YumPackageFormat();
    packageFormat.setLicense(rpmHeaderWrapper.readString(Header.HeaderTag.LICENSE, false));
    packageFormat.setVendor(rpmHeaderWrapper.readString(Header.HeaderTag.VENDOR, false));
    packageFormat.setGroup(rpmHeaderWrapper.readString(Header.HeaderTag.GROUP, true));
    packageFormat.setBuildHost(rpmHeaderWrapper.readString(Header.HeaderTag.BUILDHOST, false));
    packageFormat.setSourceRpm(rpmHeaderWrapper.readString(Header.HeaderTag.SOURCERPM, false));
    packageFormat.setProvides(rpmToYumPROConverter.convertProvides());
    packageFormat.setRequires(rpmToYumPROConverter.convertRequires());
    packageFormat.setObsoletes(rpmToYumPROConverter.convertObsoletes());
    packageFormat.setConflicts(rpmToYumPROConverter.convertConflicts());
    packageFormat.setHeaderStart(rpmHeaderWrapper.getHeader().getStartPos());
    packageFormat.setHeaderEnd(rpmHeaderWrapper.getHeader().getEndPos());
    return packageFormat;
  }

  private String createFilename(final YumPackage yumPackage) {
    return yumPackage.getArch() +
      "/" + yumPackage.getName() +
      "-" + yumPackage.getVersion().getVer() +
      "-" + yumPackage.getVersion().getRel() +
      "." + yumPackage.getArch() +
      ".rpm";
  }

  private YumPackageSize readRpmSize() throws InvalidRpmHeaderException {
    final YumPackageSize yumPackageSize = new YumPackageSize();
    yumPackageSize.setInstalled(rpmHeaderWrapper.readInteger(Header.HeaderTag.SIZE, true));
    yumPackageSize.setArchive(rpmHeaderWrapper.readInteger(Header.HeaderTag.ARCHIVESIZE, false));
    return yumPackageSize;
  }

  private YumPackageTime readRpmTime() throws InvalidRpmHeaderException {
    final YumPackageTime yumTime = new YumPackageTime();
    yumTime.setBuild(rpmHeaderWrapper.readInteger(Header.HeaderTag.BUILDTIME, true));
    return yumTime;
  }

  private YumPackageVersion readRpmVersion() throws InvalidRpmHeaderException {
    final YumPackageVersion yumPackageVersion = new YumPackageVersion();
    yumPackageVersion.setEpoch(rpmHeaderWrapper.readInteger(Header.HeaderTag.EPOCH, false));
    yumPackageVersion.setVer(rpmHeaderWrapper.readString(Header.HeaderTag.VERSION, true));
    yumPackageVersion.setRel(rpmHeaderWrapper.readString(Header.HeaderTag.RELEASE, true));
    return yumPackageVersion;
  }

  private String calculateArch(String arch, String sourceRpm) {
    if (isSourceRpm(sourceRpm)) {
      arch = "src";
    }
    return arch;
  }

  private boolean isSourceRpm(String sourceRpm) {
    return isBlank(sourceRpm) || NO_SOURCE_RPM_VALUE.equals(sourceRpm);
  }
}
