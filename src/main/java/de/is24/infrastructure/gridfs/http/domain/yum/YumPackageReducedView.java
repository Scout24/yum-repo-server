package de.is24.infrastructure.gridfs.http.domain.yum;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;


public class YumPackageReducedView {
  private YumPackage yumPackage;

  public YumPackageReducedView(YumPackage yumPackage) {
    this.yumPackage = yumPackage;
  }

  public String getName() {
    return yumPackage.getName();
  }

  public String getArch() {
    return yumPackage.getArch();
  }

  public YumPackageVersion getVersion() {
    return yumPackage.getVersion();
  }

  public String getDescription() {
    return yumPackage.getDescription();
  }

  public String getUrl() {
    return yumPackage.getUrl();
  }

  public YumPackageLocation getLocation() {
    return yumPackage.getLocation();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if ((o == null) || (this.getClass() != o.getClass())) {
      return false;
    }

    YumPackageReducedView other = (YumPackageReducedView) o;
    return new EqualsBuilder().append(getName(), other.getName())
      .append(getArch(), other.getArch())
      .append(getVersion(), other.getVersion())
      .append(getDescription(), other.getDescription())
      .append(getUrl(), other.getUrl())
      .append(getLocation(), other.getLocation())
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(getName())
      .append(getArch())
      .append(getVersion())
      .append(getDescription())
      .append(getUrl())
      .append(getLocation())
      .toHashCode();
  }

}
