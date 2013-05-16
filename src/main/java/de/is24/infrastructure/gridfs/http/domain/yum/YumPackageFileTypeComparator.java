package de.is24.infrastructure.gridfs.http.domain.yum;

import java.util.Comparator;


public class YumPackageFileTypeComparator implements Comparator<YumPackageFileType> {
  @Override
  public int compare(final YumPackageFileType type, final YumPackageFileType otherType) {
    return type.getFilelistsOrder().compareTo(otherType.getFilelistsOrder());
  }
}
