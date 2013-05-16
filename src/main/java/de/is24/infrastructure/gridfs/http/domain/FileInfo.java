package de.is24.infrastructure.gridfs.http.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mongodb.DBObject;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import java.util.Date;
import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;
import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.apache.commons.lang.builder.ToStringStyle.SHORT_PREFIX_STYLE;


public class FileInfo implements SizeProvider {
  private String arch;
  private String repo;
  private String filename;
  private long size;
  private Date lastModified;

  FileInfo() {
  }

  public FileInfo(DBObject file) {
    this.filename = substringAfterLast((String) file.get("filename"), "/");
    this.arch = getMetadata(file, "arch");
    this.repo = getMetadata(file, "repo");
    this.size = (Long) file.get("length");
    this.lastModified = (Date) file.get("uploadDate");
  }

  public String getFilename() {
    return filename;
  }

  public String getArch() {
    return arch;
  }

  public long getSize() {
    return size;
  }

  public Date getLastModified() {
    return lastModified;
  }

  public String getRepo() {
    return repo;
  }

  @JsonIgnore
  public String getFormattedLength() {
    return byteCountToDisplaySize(size);
  }

  private String getMetadata(DBObject file, String key) {
    DBObject metadata = (DBObject) file.get("metadata");
    return (String) metadata.get(key);
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(filename).append(repo).toHashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (this.getClass() != o.getClass()) {
      return false;
    }

    FileInfo other = (FileInfo) o;
    return new EqualsBuilder().append(filename, other.filename).append(repo, other.repo).isEquals();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, SHORT_PREFIX_STYLE).append(arch).append(filename).append(repo).toString();
  }

}
