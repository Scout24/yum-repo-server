package de.is24.infrastructure.gridfs.http.domain.yum;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import java.util.Date;


public class YumPackageTime {
  private Integer file;
  private Integer build;

  public Integer getFile() {
    return file;
  }

  public void setFile(final Integer file) {
    this.file = file;
  }

  public Integer getBuild() {
    return build;
  }

  public void setBuild(final Integer build) {
    this.build = build;
  }

  public Date getBuildAsDate() {
    if (build != null) {
      return new Date(build * 1000L);
    }

    return null;
  }


  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }

    final YumPackageTime other = (YumPackageTime) o;
    return new EqualsBuilder().append(file, other.file).append(build, other.build).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(file).append(build).toHashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).append(file).append(build).toString();
  }
}
