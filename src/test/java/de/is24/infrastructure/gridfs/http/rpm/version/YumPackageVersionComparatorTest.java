package de.is24.infrastructure.gridfs.http.rpm.version;

import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageVersion;
import org.junit.Test;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class YumPackageVersionComparatorTest {

  private final YumPackageVersionComparator comparator = new YumPackageVersionComparator();

  @Test
  public void compare() throws Exception {
    assertCompare(0, null, null);
    assertCompare(-1, null, new YumPackageVersion());
    assertCompare(-1, version(0, "1", "0"), version(1, "0", "0"));
    assertCompare(-1, version(0, "2", "5"), version(0, "10", "0"));
    assertCompare(0, version(0, null, "0"), version(0, null, "0"));
    assertCompare(-1, version(0, null, "2"), version(0, null, "10"));
    assertCompare(-1, version(0, null, null), version(0, null, "10"));
  }

  private YumPackageVersion version(int epoch, String ver, String rel) {
    YumPackageVersion obj = new YumPackageVersion();
    obj.setEpoch(epoch);
    obj.setVer(ver);
    obj.setRel(rel);
    return obj;
  }

  private void assertCompare(int expected, YumPackageVersion o1, YumPackageVersion o2) {
    assertThat(normalize(comparator.compare(o1, o2)), is(expected));
    assertThat(normalize(comparator.compare(o2, o1)), is(0 - expected));
  }

  private int normalize(int value) {
    return value / max(abs(value), 1);
  }
}
