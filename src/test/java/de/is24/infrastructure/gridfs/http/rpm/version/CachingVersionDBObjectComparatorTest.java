package de.is24.infrastructure.gridfs.http.rpm.version;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.junit.Test;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CachingVersionDBObjectComparatorTest {

  private final CachingVersionDBObjectComparator comparator = new CachingVersionDBObjectComparator();

  @Test
  public void compare() throws Exception {
    assertCompare(0, null, null);
    assertCompare(-1, null, new Object());
    assertCompare(-1, version(0, "1", "0"), version(1, "0", "0"));
    assertCompare(-1, version(0, "2", "5"), version(0, "10", "0"));
    assertCompare(0, version(0, null, "0"), version(0, null, "0"));
    assertCompare(-1, version(0, null, "2"), version(0, null, "10"));
    assertCompare(-1, version(0, null, null), version(0, null, "10"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void failForNonDBObjects() throws Exception {
    comparator.compare(new Object(), version(0, "1", "0"));
  }

  private DBObject version(int epoch, String ver, String rel) {
    DBObject obj = new BasicDBObject();
    obj.put("epoch", epoch);
    obj.put("ver", ver);
    obj.put("rel", rel);
    return obj;
  }

  private void assertCompare(int expected, Object o1, Object o2) {
    assertThat(normalize(comparator.compare(o1, o2)), is(expected));
    assertThat(normalize(comparator.compare(o2, o1)), is(0 - expected));
  }

  private int normalize(int value) {
    return value / max(abs(value), 1);
  }
}
