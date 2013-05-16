package de.is24.infrastructure.gridfs.http.domain.yum;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;


public class YumPackageVersion {
  private int epoch;
  private String ver;
  private String rel;

  public int getEpoch() {
    return epoch;
  }

  public void setEpoch(final int epoch) {
    this.epoch = epoch;
  }

  public String getVer() {
    return ver;
  }

  public void setVer(final String ver) {
    this.ver = ver;
  }

  public String getRel() {
    return rel;
  }

  public void setRel(final String rel) {
    this.rel = rel;

  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if ((o == null) || (this.getClass() != o.getClass())) {
      return false;
    }

    YumPackageVersion other = (YumPackageVersion) o;
    return new EqualsBuilder().append(epoch, other.epoch).append(ver, other.ver).append(rel, other.rel).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(epoch).append(ver).append(rel).toHashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).append(epoch).append(ver).append(rel).toString();
  }
}
