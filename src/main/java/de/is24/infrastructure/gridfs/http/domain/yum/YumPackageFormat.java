package de.is24.infrastructure.gridfs.http.domain.yum;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import java.util.List;


public class YumPackageFormat {
  private String license;
  private String vendor;
  private String group;
  private String buildHost;
  private String sourceRpm;
  private int headerStart;
  private int headerEnd;
  private List<YumPackageFormatEntry> provides;
  private List<YumPackageRequirement> requires;
  private List<YumPackageFormatEntry> obsoletes;
  private List<YumPackageFormatEntry> conflicts;

  public String getLicense() {
    return license;
  }

  public void setLicense(final String license) {
    this.license = license;
  }

  public String getVendor() {
    return vendor;
  }

  public void setVendor(final String vendor) {
    this.vendor = vendor;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(final String group) {
    this.group = group;
  }

  public String getBuildHost() {
    return buildHost;
  }

  public void setBuildHost(final String buildHost) {
    this.buildHost = buildHost;
  }

  public String getSourceRpm() {
    return sourceRpm;
  }

  public void setSourceRpm(final String sourceRpm) {
    this.sourceRpm = sourceRpm;
  }

  public int getHeaderStart() {
    return headerStart;
  }

  public void setHeaderStart(final int headerStart) {
    this.headerStart = headerStart;
  }

  public int getHeaderEnd() {
    return headerEnd;
  }

  public void setHeaderEnd(final int headerEnd) {
    this.headerEnd = headerEnd;
  }

  public List<YumPackageFormatEntry> getProvides() {
    return provides;
  }

  public void setProvides(final List<YumPackageFormatEntry> provides) {
    this.provides = provides;
  }

  public List<YumPackageRequirement> getRequires() {
    return requires;
  }

  public void setRequires(final List<YumPackageRequirement> requires) {
    this.requires = requires;
  }

  public List<YumPackageFormatEntry> getObsoletes() {
    return obsoletes;
  }

  public void setObsoletes(final List<YumPackageFormatEntry> obsoletes) {
    this.obsoletes = obsoletes;
  }

  public List<YumPackageFormatEntry> getConflicts() {
    return conflicts;
  }

  public void setConflicts(final List<YumPackageFormatEntry> conflicts) {
    this.conflicts = conflicts;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }

    final YumPackageFormat other = (YumPackageFormat) o;
    return new EqualsBuilder().append(license, other.license)
      .append(vendor, other.vendor)
      .append(group, other.group)
      .append(buildHost, other.buildHost)
      .append(sourceRpm, other.sourceRpm)
      .append(headerStart, other.headerStart)
      .append(requires, other.requires)
      .append(provides, other.provides)
      .append(obsoletes, other.obsoletes)
      .append(conflicts, other.conflicts)
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(license)
      .append(vendor)
      .append(group)
      .append(buildHost)
      .append(sourceRpm)
      .append(headerStart)
      .append(requires)
      .append(provides)
      .append(obsoletes)
      .append(conflicts)
      .toHashCode();
  }


  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).append(license)
      .append(vendor)
      .append(group)
      .append(buildHost)
      .append(sourceRpm)
      .append(headerStart)
      .append(requires)
      .append(provides)
      .append(obsoletes)
      .append(conflicts)
      .toString();
  }
}
