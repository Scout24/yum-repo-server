package de.is24.infrastructure.gridfs.http.storage;

import com.mongodb.gridfs.GridFSDBFile;
import de.is24.infrastructure.gridfs.http.domain.YumEntry;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackage;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import static org.apache.commons.lang.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang.builder.HashCodeBuilder.reflectionHashCode;


public class FileDescriptor {
  private String repo;
  private String arch;
  private String filename;

  /**
   * create a GridFsFileDescriptor from a String path
   *
   * @param path following the pattern repo/arch/filename or more detailed contains two slashes
   * @throws IllegalArgumentException if path does not follow pattern repo/arch/filename
   */
  public FileDescriptor(String path) {
    String[] split = path.split("/");
    if (split.length != 3) {
      throw new IllegalArgumentException("path " + path +
        " does not follow pattern repo/arch/filename");
    }

    this.repo = split[0];
    this.arch = split[1];
    this.filename = split[2];
  }

  public FileDescriptor(String repo, String arch, String filename) {
    this.repo = repo;
    this.arch = arch;
    this.filename = filename;
  }

  public FileDescriptor(YumEntry entry) {
    this(entry.getRepo(), entry.getYumPackage());
  }

  public FileDescriptor(GridFSDBFile gridFSDBFile) {
    this(gridFSDBFile.getFilename());
  }

  public FileDescriptor(String repo, YumPackage yumPackage) {
    this.repo = repo;

    String href = yumPackage.getLocation().getHref();
    String[] split = href.split("/");
    if (split.length == 2) {
      this.arch = split[0];
      this.filename = split[1];
    } else {
      throw new IllegalArgumentException("location href " + href +
        " does not follow pattern arch/filename");
    }
  }

  public FileDescriptor(FileStorageItem storageItem) {
    this(storageItem.getFilename());
  }


  public String getPath() {
    return repo + "/" + arch + "/" + filename;
  }

  public String getRepo() {
    return repo;
  }

  public String getArch() {
    return arch;
  }

  public String getFilename() {
    return filename;
  }

  public void setRepo(String repo) {
    this.repo = repo;
  }

  @Override
  public int hashCode() {
    return reflectionHashCode(this);
  }

  @Override
  public boolean equals(Object obj) {
    return reflectionEquals(this, obj);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).append(repo)
      .append(arch)
      .append(filename)
      .toString();
  }

}
