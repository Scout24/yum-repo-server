package de.is24.infrastructure.gridfs.http.rpm;

import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFileType;
import static de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFileType.DIR;
import static de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFileType.FILE;
import static de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFileType.GHOST;


public final class RpmFileTypeTagToYumConverter {
  private RpmFileTypeTagToYumConverter() {
  }

  private static final int RPM_DIR_MOD = 16877;
  private static final int RPM_GHOST_FLAG = 64;

  public static YumPackageFileType convert(int flag, int mode) {
    if (isDir(mode)) {
      return DIR;
    }
    if (isGhost(flag)) {
      return GHOST;
    } else {
      return FILE;
    }
  }

  private static boolean isGhost(final int flag) {
    return RPM_GHOST_FLAG == flag;
  }

  private static boolean isDir(final int mode) {
    return RPM_DIR_MOD == mode;
  }
}
