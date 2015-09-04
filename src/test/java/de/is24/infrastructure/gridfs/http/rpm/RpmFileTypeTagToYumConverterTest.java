package de.is24.infrastructure.gridfs.http.rpm;

import org.junit.Test;
import static de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFileType.DIR;
import static de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFileType.FILE;
import static de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFileType.GHOST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;


public class RpmFileTypeTagToYumConverterTest {
  @Test
  public void detectDir() {
    // 16877 corresponds to drwxr-xr-x which is not the only possible mode for directories
    assertThat(RpmFileTypeTagToYumConverter.convert(64, 16877), equalTo(DIR));
  }

  @Test
  public void detectDirByMasking() {
    assertThat(RpmFileTypeTagToYumConverter.convert(64, 0x4000), equalTo(DIR));
  }

  @Test
  public void isGhost() {
    assertThat(RpmFileTypeTagToYumConverter.convert(64, 0), equalTo(GHOST));
  }

  @Test
  public void ghostFromMask() {
    assertThat(RpmFileTypeTagToYumConverter.convert(0x4f, 0), equalTo(GHOST));
  }

  @Test
  public void isFile() {
    assertThat(RpmFileTypeTagToYumConverter.convert(0, 0), equalTo(FILE));
  }
}
