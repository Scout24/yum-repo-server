package de.is24.infrastructure.gridfs.http.domain.yum;

public enum YumPackageFileType {
  FILE("f", 2),
  DIR("d", 1),
  GHOST("g", 0);

  private final String typeChar;
  private final int filelistsOrder;

  YumPackageFileType(final String typeChar, final int filelistsOrder) {
    this.typeChar = typeChar;
    this.filelistsOrder = filelistsOrder;
  }

  public String getTypeChar() {
    return typeChar;
  }

  public Integer getFilelistsOrder() {
    return filelistsOrder;
  }
}
