package de.is24.infrastructure.gridfs.http.domain.yum;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.is24.infrastructure.gridfs.http.rpm.version.YumPackageVersionComparator;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;


public class YumPackageReducedView implements Comparable<YumPackageReducedView> {
  public static final YumPackageVersionComparator YUM_PACKAGE_VERSION_COMPARATOR = new YumPackageVersionComparator();
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

  public YumPackageSize getSize() {
    return yumPackage.getSize();
  }

  @JsonIgnore
  public String getFormattedLength() {
    return yumPackage.getSize().getPackagedAsString();
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
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(getName()).append(getArch()).append(getVersion()).toHashCode();
  }


  @Override
  public int compareTo(YumPackageReducedView o) {
    return new CompareToBuilder().append(getName(), o.getName())
      .append(getArch(), o.getArch())
      .append(getVersion(), o.getVersion(), YUM_PACKAGE_VERSION_COMPARATOR)
      .toComparison();
  }
}
