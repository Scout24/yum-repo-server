package de.is24.infrastructure.gridfs.http.mongo.util;

import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.Paths;
import de.flapdoodle.embed.process.distribution.ArchiveType;
import de.flapdoodle.embed.process.distribution.BitSize;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.distribution.Platform;


public class MavenPaths extends Paths {
  private final String groupId;
  private final String artifactId;

  public MavenPaths(Command command, String groupId, String artifactId) {
    super(command);
    this.groupId = groupId;
    this.artifactId = artifactId;
  }

  @Override
  public String getPath(Distribution distribution) {
    String sversion = getVersionPart(distribution.getVersion());
    String sversionPostfix = "";

    ArchiveType archiveType = getArchiveType(distribution);
    String sarchiveType;
    switch (archiveType) {
      case TGZ: {
        sarchiveType = "tgz";
        break;
      }

      case ZIP: {
        sarchiveType = "zip";
        break;
      }

      default: {
        throw new IllegalArgumentException("Unknown ArchiveType " + archiveType);
      }
    }

    String splatform;
    switch (distribution.getPlatform()) {
      case Linux: {
        splatform = "linux";
        break;
      }

      case Windows: {
        splatform = "win32";
        break;
      }

      case OS_X: {
        splatform = "osx";
        break;
      }

      default: {
        throw new IllegalArgumentException("Unknown Platform " + distribution.getPlatform());
      }
    }

    String sbitSize;
    switch (distribution.getBitsize()) {
      case B32: {
        switch (distribution.getPlatform()) {
          case Linux: {
            sbitSize = "i686";
            break;
          }

          case Windows: {
            sbitSize = "i386";
            break;
          }

          case OS_X: {
            sbitSize = "i386";
            break;
          }

          default: {
            throw new IllegalArgumentException("Unknown Platform " + distribution.getPlatform());
          }
        }
        break;
      }

      case B64: {
        sbitSize = "x86_64";
        break;
      }

      default: {
        throw new IllegalArgumentException("Unknown BitSize " + distribution.getBitsize());
      }
    }

    if ((distribution.getBitsize() == BitSize.B64) && (distribution.getPlatform() == Platform.Windows)) {
      if (useWindows2008PlusVersion()) {
        sversionPostfix = "-2008plus";
      }
    }

    return getGroupIdPath() + "/" + artifactId + "/" + sversion + "/" + artifactId + "-" + sversion +
      "-" + splatform + "-" + sbitSize + sversionPostfix + "." + sarchiveType;
  }

  private String getGroupIdPath() {
    return groupId.replace(".", "/");
  }
}
