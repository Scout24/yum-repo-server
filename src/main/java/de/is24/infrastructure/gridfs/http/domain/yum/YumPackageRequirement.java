package de.is24.infrastructure.gridfs.http.domain.yum;

public class YumPackageRequirement extends YumPackageFormatEntry {
  private Boolean pre;

  public Boolean isPre() {
    return pre;
  }

  public void setPre(Boolean pre) {
    this.pre = pre;
  }

}
