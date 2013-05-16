package de.is24.infrastructure.gridfs.http.metadata.generation;

import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFile;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFileType;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class PrimaryDirNameMatcher extends BaseMatcher<YumPackageFile> {

  private YumPackageFileType type;

  public PrimaryDirNameMatcher(YumPackageFileType type) {
    this.type = type;
  }

  @Override
  public boolean matches(Object item) {
    YumPackageFile file = (YumPackageFile) item;
    return type.equals(file.getType()) && isPrimaryDirName(file.getDir());
  }

  public static boolean isPrimaryDirName(String dir) {
    return dir.contains("bin/") || dir.startsWith("/etc/");
  }

  @Override
  public void describeTo(Description description) {
  }
}
