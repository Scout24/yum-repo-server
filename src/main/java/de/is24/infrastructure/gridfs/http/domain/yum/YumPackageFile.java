package de.is24.infrastructure.gridfs.http.domain.yum;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import static de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFileType.DIR;
import static org.apache.commons.lang.StringUtils.isBlank;


public class YumPackageFile {
  private YumPackageFileType type;
  private String name;
  private String dir;

  public YumPackageFile(final YumPackageFileType type, final String name, final String dir) {
    this.type = type;
    this.name = name;
    this.dir = dir;
  }

  public YumPackageFileType getType() {
    return type;
  }

  public void setType(final YumPackageFileType type) {
    this.type = type;
  }

  public String getName() {
    return (isBlank(name) && (type == DIR)) ? dir : name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getDir() {
    return dir;
  }

  public void setDir(final String dir) {
    this.dir = dir;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }

    final YumPackageFile other = (YumPackageFile) o;
    return new EqualsBuilder().append(type, other.type).append(name, other.name).append(dir, other.dir).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(type).append(name).append(dir).toHashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).append(type).append(name).append(dir).toString();
  }
}
