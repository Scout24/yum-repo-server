package de.is24.infrastructure.gridfs.http.rpm;

import de.is24.infrastructure.gridfs.http.exception.InvalidRpmHeaderException;
import org.junit.Test;

import static de.is24.infrastructure.gridfs.http.rpm.RpmPROTagFlagsToYumConverter.convert;
import static de.is24.infrastructure.gridfs.http.rpm.RpmPROTagFlagsToYumConverter.isPre;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class RpmPROTagFlagsToYumConverterTest {

  @Test
  public void rpmTagFlagTranslation() throws Exception {
    assertThat(convert(0), nullValue());
    assertThat(convert(2), equalTo("LT"));
    assertThat(convert(4), equalTo("GT"));
    assertThat(convert(8), equalTo("EQ"));
    assertThat(convert(10), equalTo("LE"));
    assertThat(convert(16777226), equalTo("LE"));
    assertThat(convert(12), equalTo("GE"));
  }

  @Test
  public void rpmFlagPreRecognition() throws Exception {
    assertThat(isPre(10), is(false));
    assertThat(isPre(16777226), is(false));
    assertThat(isPre(1280), is(true));
  }

  @Test(expected = InvalidRpmHeaderException.class)
  public void unsupportedVersionFlagsThrowsException() throws Exception {
    convert(-1);
  }
}
