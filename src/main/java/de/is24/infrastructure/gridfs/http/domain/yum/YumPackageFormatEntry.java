package de.is24.infrastructure.gridfs.http.domain.yum;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;


public class YumPackageFormatEntry {
  private String name;
  private String flags;
  private YumPackageVersion version;

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getFlags() {
    return flags;
  }

  public void setFlags(final String flags) {
    this.flags = flags;
  }

  public YumPackageVersion getVersion() {
    return version;
  }

  public void setVersion(final YumPackageVersion version) {
    this.version = version;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }

    final YumPackageFormatEntry other = (YumPackageFormatEntry) o;
    return new EqualsBuilder().append(name, other.name)
      .append(flags, other.flags)
      .append(version, other.version)
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(name).append(flags).append(version).toHashCode();
  }


  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).append(name)
      .append(flags)
      .append(version)
      .toString();
  }
}
