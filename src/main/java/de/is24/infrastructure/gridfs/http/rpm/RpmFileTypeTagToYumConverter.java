package de.is24.infrastructure.gridfs.http.rpm;

import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFileType;
import static de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFileType.DIR;
import static de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFileType.FILE;
import static de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFileType.GHOST;


public final class RpmFileTypeTagToYumConverter {
  // from man 2 stat
  private static final int S_IFMT = 0170000; // bit mask for the file type bit fields
  private static final int S_IFSOCK = 0140000; // socket
  private static final int S_IFLNK = 0120000; //  symbolic link
  private static final int S_IFREG = 0100000; //  regular file
  private static final int S_IFBLK = 0060000; //  block device
  private static final int S_IFDIR = 0040000; //  directory
  private static final int S_IFCHR = 0020000; //  character device
  private static final int S_IFIFO = 0010000; //  FIFO
  private static final int S_ISUID = 0004000; //  set UID bit
  private static final int S_ISGID = 0002000; //  set-group-ID bit (see below)
  private static final int S_ISVTX = 0001000; //  sticky bit (see below)
  private static final int S_IRWXU = 00700; //  mask for file owner permissions
  private static final int S_IRUSR = 00400; //  owner has read permission
  private static final int S_IWUSR = 00200; //  owner has write permission
  private static final int S_IXUSR = 00100; //  owner has execute permission
  private static final int S_IRWXG = 00070; // mask for group permissions
  private static final int S_IRGRP = 00040; //  group has read permission
  private static final int S_IWGRP = 00020; //  group has write permission
  private static final int S_IXGRP = 00010; //  group has execute permission
  private static final int S_IRWXO = 00007; //  mask for permissions for others (not in group)
  private static final int S_IROTH = 00004; //  others have read permission
  private static final int S_IWOTH = 00002; //  others have write permission
  private static final int S_IXOTH = 00001; //  others have execute permission

  // from https://refspecs.linuxbase.org/LSB_3.1.0/LSB-Core-generic/LSB-Core-generic/pkgformat.html
  private static final int RPMFILE_CONFIG = (1 << 0);
  private static final int RPMFILE_DOC = (1 << 1);
  private static final int RPMFILE_DONOTUSE = (1 << 2);
  private static final int RPMFILE_MISSINGOK = (1 << 3);
  private static final int RPMFILE_NOREPLACE = (1 << 4);
  private static final int RPMFILE_SPECFILE = (1 << 5);
  private static final int RPMFILE_GHOST = (1 << 6);
  private static final int RPMFILE_LICENSE = (1 << 7);
  private static final int RPMFILE_README = (1 << 8);
  private static final int RPMFILE_EXCLUDE = (1 << 9);


  private RpmFileTypeTagToYumConverter() {
  }

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
    return (flag & RPMFILE_GHOST) != 0;
  }

  private static boolean isDir(final int mode) {
    return (mode & S_IFMT) == S_IFDIR;
  }
}
