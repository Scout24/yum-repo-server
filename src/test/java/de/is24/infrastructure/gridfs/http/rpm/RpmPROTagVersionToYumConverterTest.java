package de.is24.infrastructure.gridfs.http.rpm;

import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageVersion;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class RpmPROTagVersionToYumConverterTest {

  @Test
  public void parse() throws Exception {
    final YumPackageVersion yumPackageVersion = RpmPROTagVersionToYumConverter.convert("1:1.1-2.2");
    assertThat(yumPackageVersion.getEpoch(), equalTo(1));
    assertThat(yumPackageVersion.getVer(), equalTo("1.1"));
    assertThat(yumPackageVersion.getRel(), equalTo("2.2"));
  }

  @Test
  public void parseWithoutRelease() throws Exception {
    final YumPackageVersion yumPackageVersion = RpmPROTagVersionToYumConverter.convert("1:1.1");
    assertThat(yumPackageVersion.getEpoch(), equalTo(1));
    assertThat(yumPackageVersion.getVer(), equalTo("1.1"));
    assertThat(yumPackageVersion.getRel(), equalTo(""));
  }

  @Test
  public void parseWithoutEpoch() throws Exception {
    final YumPackageVersion yumPackageVersion = RpmPROTagVersionToYumConverter.convert("1.1-2.2");
    assertThat(yumPackageVersion.getEpoch(), equalTo(0));
    assertThat(yumPackageVersion.getVer(), equalTo("1.1"));
    assertThat(yumPackageVersion.getRel(), equalTo("2.2"));
  }

  @Test
  public void parseInvalid() throws Exception {
    final YumPackageVersion yumPackageVersion = RpmPROTagVersionToYumConverter.convert(":-");
    assertThat(yumPackageVersion.getEpoch(), equalTo(0));
    assertThat(yumPackageVersion.getVer(), equalTo(""));
    assertThat(yumPackageVersion.getRel(), equalTo(""));
  }

  @Test
  public void parseInvalidEpoch() throws Exception {
    final YumPackageVersion yumPackageVersion = RpmPROTagVersionToYumConverter.convert("a:1.1-2.2");
    assertThat(yumPackageVersion.getEpoch(), equalTo(0));
    assertThat(yumPackageVersion.getVer(), equalTo("1.1"));
    assertThat(yumPackageVersion.getRel(), equalTo("2.2"));
  }
}
