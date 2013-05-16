package de.is24.infrastructure.gridfs.http.rpm.version;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;


public class RpmVersionSegmentComparatorTest {
  private final RpmVersionSegmentComparator comparator = new RpmVersionSegmentComparator();

  @Test
  public void stringSegmentsAreComparedAsString() throws Exception {
    assertCompare(-1, asList("1", "2", "ab"), asList("1", "2", "cd"));
    assertCompare(-1, asList("1", "2", "0cd"), asList("1", "2", "ab"));
    assertCompare(-1, asList("1", "2", "ab"), asList("1", "2", "abc"));
  }

  @Test
  public void numericSegmentsAreComparedAsNumbers() throws Exception {
    assertCompare(-1, asList("1", "20", "ab"), asList("1", "30", "ab"));
    assertCompare(-1, asList("1", "20", "ab"), asList("1", "030", "ab"));
    assertCompare(-1, asList("1", "0003", "ab"), asList("1", "020", "ab"));
    assertCompare(-1, asList("1", "0030", "ab"), asList("1", "0300", "ab"));
    assertCompare(0, asList("1", "00300", "ab"), asList("1", "0300", "ab"));
  }

  @Test
  public void equalSegmentsAreEqual() throws Exception {
    assertCompare(0, asList("0", "0", "0"), asList("0", "0", "0"));
    assertCompare(0, asList("a", "0", "0a"), asList("a", "0", "0a"));
    assertCompare(0, null, null);
    assertCompare(0, null, new ArrayList<String>());
  }

  @Test
  public void bigNumbersAreComparable() throws Exception {
    String numberRepresentingTimestamp = "20130301145855";
    String numberWith20digits = "12345678901234567890";
    String numberWith60digits = numberWith20digits + numberWith20digits + numberWith20digits;

    assertCompare(-1, asList("1"), asList(numberRepresentingTimestamp));
    assertCompare(-1, asList("1"), asList(numberWith20digits));
    assertCompare(-1, asList("1"), asList(numberWith60digits));
    assertCompare(-1, asList(numberWith20digits), asList(numberWith60digits));
  }

  @Test
  public void lessSegmentsAreSmaller() throws Exception {
    assertCompare(-1, null, asList("1"));
    assertCompare(-1, new ArrayList<String>(), asList("1"));
    assertCompare(-1, asList("1", ""), asList("1", "1"));
    assertCompare(-1, asList("0", "0"), asList("0", "0", "0"));
  }

  @Test
  public void characterIsSmallerThanNumber() throws Exception {
    assertCompare(-1, asList("1", "2", "a"), asList("1", "2", "3"));
  }

  private void assertCompare(int result, List<String> o1, List<String> o2) {
    int compare1 = comparator.compare(o1, o2);
    assertThat(compare1 / max(abs(compare1), 1), is(result));

    int compare2 = comparator.compare(o2, o1);
    assertThat(compare2 / max(abs(compare2), 1), is(0 - result));
  }
}
