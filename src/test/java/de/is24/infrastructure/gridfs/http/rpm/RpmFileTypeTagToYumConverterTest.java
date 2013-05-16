package de.is24.infrastructure.gridfs.http.rpm;

import org.junit.Test;

import static de.is24.infrastructure.gridfs.http.domain.yum.YumPackageFileType.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class RpmFileTypeTagToYumConverterTest {
  @Test
  public void isDir() {
    assertThat(RpmFileTypeTagToYumConverter.convert(64, 16877), equalTo(DIR));
  }

  @Test
  public void isGhost() {
    assertThat(RpmFileTypeTagToYumConverter.convert(64, 0), equalTo(GHOST));
  }

  @Test
  public void isFile() {
    assertThat(RpmFileTypeTagToYumConverter.convert(0, 0), equalTo(FILE));
  }
}
