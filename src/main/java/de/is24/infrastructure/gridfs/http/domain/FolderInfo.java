package de.is24.infrastructure.gridfs.http.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mongodb.DBObject;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import java.util.Date;
import java.util.Set;
import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;
import static org.apache.commons.lang.builder.ToStringStyle.SHORT_PREFIX_STYLE;


public class FolderInfo implements SizeProvider {
  private long size;
  private String name;
  private Date lastModified;
  private boolean external;
  private String href;
  private String target;
  private Set<String> tags;

  private FolderInfo() {
  }

  public FolderInfo(DBObject object) {
    this.name = (String) object.get("_id");
    this.size = (Long) object.get("length");
    this.lastModified = (Date) object.get("uploadDate");
    this.href = name;
  }

  public static FolderInfo fromRepoEntry(RepoEntry repoEntry, long size) {
    FolderInfo folderInfo = new FolderInfo();
    folderInfo.size = size;
    folderInfo.name = repoEntry.getName();
    folderInfo.lastModified = repoEntry.getLastModified();
    folderInfo.external = repoEntry.isExternal();
    folderInfo.href = repoEntry.isExternal() ? repoEntry.getTarget() : folderInfo.name;
    folderInfo.target = repoEntry.getTarget();
    folderInfo.tags = repoEntry.getTags();
    return folderInfo;
  }

  public long getSize() {
    return size;
  }

  public String getName() {
    return name;
  }

  public String getHref() {
    return href;
  }

  public String getTarget() {
    return target;
  }

  public Set<String> getTags() {
    return tags;
  }

  @JsonIgnore
  public String getFormattedSize() {
    return byteCountToDisplaySize(size);
  }

  public Date getLastModified() {
    return lastModified;
  }

  @JsonIgnore
  public boolean isExternal() {
    return external;
  }

  public void setLastModified(Date lastModified) {
    this.lastModified = lastModified;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(name).toHashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (this.getClass() != o.getClass()) {
      return false;
    }

    FolderInfo other = (FolderInfo) o;
    return new EqualsBuilder().append(name, other.name).isEquals();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, SHORT_PREFIX_STYLE).append(name).toString();
  }

  public void setTags(Set<String> tags) {
    this.tags = tags;
  }
}
