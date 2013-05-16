package de.is24.infrastructure.gridfs.http.domain.yum;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.springframework.data.mongodb.core.index.Indexed;
import java.util.List;


public class YumPackage {
  @Indexed
  private String name;
  private String arch;
  private String summary;
  private YumPackageVersion version;
  private YumPackageChecksum checksum;
  private String description;
  private String packager;
  private String url;
  private YumPackageTime time;
  private YumPackageSize size;
  private YumPackageLocation location;
  private YumPackageFormat packageFormat;
  private List<YumPackageChangeLog> changeLogs;
  private YumPackageDir[] packageDirs;

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getArch() {
    return arch;
  }

  public void setArch(final String arch) {
    this.arch = arch;
  }

  public YumPackageChecksum getChecksum() {
    return checksum;
  }

  public void setChecksum(final YumPackageChecksum checksum) {
    this.checksum = checksum;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(final String summary) {
    this.summary = summary;
  }

  public YumPackageVersion getVersion() {
    return version;
  }

  public void setVersion(final YumPackageVersion version) {
    this.version = version;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public String getPackager() {
    return packager;
  }

  public void setPackager(final String packager) {
    this.packager = packager;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(final String url) {
    this.url = url;
  }

  public YumPackageTime getTime() {
    return time;
  }

  public void setTime(final YumPackageTime time) {
    this.time = time;
  }

  public YumPackageSize getSize() {
    return size;
  }

  public void setSize(final YumPackageSize size) {
    this.size = size;
  }

  public YumPackageLocation getLocation() {
    return location;
  }

  public void setLocation(final YumPackageLocation location) {
    this.location = location;
  }

  public YumPackageFormat getPackageFormat() {
    return packageFormat;
  }

  public void setPackageFormat(final YumPackageFormat packageFormat) {
    this.packageFormat = packageFormat;
  }

  public List<YumPackageChangeLog> getChangeLogs() {
    return changeLogs;
  }

  public void setChangeLogs(final List<YumPackageChangeLog> changeLogs) {
    this.changeLogs = changeLogs;
  }

  public YumPackageDir[] getPackageDirs() {
    return packageDirs;
  }

  public void setPackageDirs(final YumPackageDir[] packageDirs) {
    this.packageDirs = packageDirs;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if ((o == null) || (this.getClass() != o.getClass())) {
      return false;
    }

    YumPackage other = (YumPackage) o;
    return new EqualsBuilder().append(name, other.name)
      .append(arch, other.arch)
      .append(checksum, other.checksum)
      .append(summary, other.summary)
      .append(version, other.version)
      .append(description, other.description)
      .append(packager, other.packager)
      .append(url, other.url)
      .append(time, other.time)
      .append(size, other.size)
      .append(location, other.location)
      .append(packageFormat, other.packageFormat)
      .append(changeLogs, other.changeLogs)
      .append(packageDirs, other.packageDirs)
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(name)
      .append(arch)
      .append(checksum)
      .append(summary)
      .append(version)
      .append(description)
      .append(packager)
      .append(url)
      .append(time)
      .append(size)
      .append(location)
      .append(packageFormat)
      .append(changeLogs)
      .append(packageDirs)
      .toHashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).append(name)
      .append(arch)
      .append(checksum)
      .append(summary)
      .append(version)
      .append(description)
      .append(packager)
      .append(url)
      .append(time)
      .append(size)
      .append(location)
      .append(packageFormat)
      .append(changeLogs)
      .append(packageDirs)
      .toString();
  }
}
