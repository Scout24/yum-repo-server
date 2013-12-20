package de.is24.infrastructure.gridfs.http.gridfs;

import de.is24.infrastructure.gridfs.http.domain.YumEntry;


public class GridFsFileDescriptor {
  private String repo;
  private String arch;
  private String filename;

  public GridFsFileDescriptor(String repo, String arch, String filename) {
    this.repo = repo;
    this.arch = arch;
    this.filename = filename;
  }

  public GridFsFileDescriptor(YumEntry entry) {
    this.repo = entry.getRepo();

    String href = entry.getYumPackage().getLocation().getHref();
    String[] split = href.split("/");
    if (split.length == 2) {
      this.arch = split[0];
      this.filename = split[1];
    } else {
      throw new IllegalArgumentException("location href " + href +
        " does not follow pattern arch/filename");
    }
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
}
