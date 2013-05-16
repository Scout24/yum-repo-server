package de.is24.infrastructure.gridfs.http.domain.yum;

import java.util.ArrayList;
import java.util.List;


public class YumPackageDir {
  private String name;
  private List<YumPackageFile> files = new ArrayList<>();

  public List<YumPackageFile> getFiles() {
    return files;
  }

  public void setFiles(List<YumPackageFile> files) {
    this.files = files;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
