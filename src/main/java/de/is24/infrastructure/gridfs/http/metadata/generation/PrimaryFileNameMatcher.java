package de.is24.infrastructure.gridfs.http.metadata.generation;

import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFile;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFileType;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import static de.is24.infrastructure.gridfs.http.metadata.generation.PrimaryDirNameMatcher.isPrimaryDirName;

public class PrimaryFileNameMatcher extends BaseMatcher<YumPackageFile> {

  private YumPackageFileType type;

  public PrimaryFileNameMatcher(YumPackageFileType type) {
    this.type = type;
  }

  @Override
  public boolean matches(Object item) {
    YumPackageFile file = (YumPackageFile) item;
    return type.equals(file.getType()) && isPrimaryFileName(file);
  }

  public static boolean isPrimaryFileName(YumPackageFile file) {
    return isPrimaryDirName(file.getDir()) || "/usr/lib/sendmail".equals(file.getDir() + file.getName());
  }

  @Override
  public void describeTo(Description description) {
  }
}
