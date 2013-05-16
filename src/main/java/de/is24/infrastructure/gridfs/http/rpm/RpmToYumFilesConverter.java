package de.is24.infrastructure.gridfs.http.rpm;

import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageDir;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFile;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFileType;
import de.is24.infrastructure.gridfs.http.exception.InvalidRpmHeaderException;
import org.apache.commons.lang.StringUtils;
import org.freecompany.redline.header.Header;

import static de.is24.infrastructure.gridfs.http.rpm.RpmFileTypeTagToYumConverter.convert;

public class RpmToYumFilesConverter {
  private final RpmHeaderWrapper rpmHeaderWrapper;

  public RpmToYumFilesConverter(final RpmHeaderWrapper rpmHeaderWrapper) {
    this.rpmHeaderWrapper = rpmHeaderWrapper;
  }

  public YumPackageDir[] readFiles() throws InvalidRpmHeaderException {
    String[] dirNames = rpmHeaderWrapper.readStrings(Header.HeaderTag.DIRNAMES, false);
    YumPackageDir[] dirs = new YumPackageDir[dirNames.length];
    int index = 0;
    for (String dirName : dirNames) {
      YumPackageDir dir = new YumPackageDir();
      dir.setName(removeTrailingSlash(dirName));
      dirs[index] = dir;
      index++;
    }

    fillDirectories(
        dirs,
        rpmHeaderWrapper.readStrings(Header.HeaderTag.BASENAMES, false),
        dirNames,
        rpmHeaderWrapper.readIntegers(Header.HeaderTag.DIRINDEXES, false),
        rpmHeaderWrapper.readIntegers(Header.HeaderTag.FILEFLAGS, false),
        rpmHeaderWrapper.readShorts(Header.HeaderTag.FILEMODES, false)
    );

    return dirs;
  }

  private void fillDirectories(final YumPackageDir[] dirs, final String[] fileNames, final String[] directoryNames, final int[] directoryIndex, final int[] flags, final short[] modes) {
    for (int i = 0; i < fileNames.length; i++) {
      YumPackageDir dir = dirs[directoryIndex[i]];
      dir.getFiles().add(createFileEntry(directoryNames[directoryIndex[i]], fileNames[i], determineFileType(flags[i], modes[i])));
    }
  }

  private String removeTrailingSlash(final String directory) {
    return directory.length() > 1 ? StringUtils.removeEnd(directory, "/") : directory;
  }

  private YumPackageFileType determineFileType(final int flag, final int mode) {
    return convert(flag, mode);
  }

  private YumPackageFile createFileEntry(final String directoryName, final String fileName, final YumPackageFileType type) {
    return new YumPackageFile(type, fileName, directoryName);
  }
}
