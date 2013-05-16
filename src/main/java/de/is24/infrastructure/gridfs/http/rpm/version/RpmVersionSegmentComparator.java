package de.is24.infrastructure.gridfs.http.rpm.version;

import org.apache.commons.lang.StringUtils;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;


public class RpmVersionSegmentComparator implements Comparator<List<String>> {
  @Override
  public int compare(List<String> o1, List<String> o2) {
    if ((o1 == null) || o1.isEmpty()) {
      return ((o2 == null) || o2.isEmpty()) ? 0 : -1;
    }
    if ((o2 == null) || o2.isEmpty()) {
      return 1;
    }

    return compareNonNullLists(o1, o2);
  }

  private int compareNonNullLists(final List<String> o1, final List<String> o2) {
    Iterator<String> iterator1 = o1.iterator();
    Iterator<String> iterator2 = o2.iterator();

    while (iterator1.hasNext() && iterator2.hasNext()) {
      String segment1 = iterator1.next();
      String segment2 = iterator2.next();

      // not-empty is always greater than empty
      if (!segment1.isEmpty() && segment2.isEmpty()) {
        return 1;
      }
      if (segment1.isEmpty() && !segment2.isEmpty()) {
        return -1;
      }

      int result = compareNonEmptySegments(segment1, segment2);
      if (result != 0) {
        return result;
      }
    }
    return compareSizeOfLists(o1, o2);
  }

  private int compareNonEmptySegments(final String segment1, final String segment2) {
    if (!(segment1.isEmpty() && segment2.isEmpty())) {
      int result = compareSegments(segment1, segment2);
      if (result == 0) {
        return 0;
      } else {
        return result;
      }
    } else {
      return 0;
    }
  }

  private int compareSizeOfLists(final List<String> o1, final List<String> o2) {
    // version with more segments than another greater if otherwise equal
    if (o1.size() == o2.size()) {
      return 0;
    } else {
      return (o1.size() > o2.size()) ? 1 : -1;
    }
  }

  private int compareSegments(final String segment1, final String segment2) {
    boolean isNumeric1 = StringUtils.isNumeric(segment1);
    boolean isNumeric2 = StringUtils.isNumeric(segment2);

    // numeric is always greater than non-numeric
    if (isNumeric1 && !isNumeric2) {
      return 1;
    }
    if (!isNumeric1 && isNumeric2) {
      return -1;
    }

    return compareStrings(segment1, segment2, isNumeric1, isNumeric2);
  }

  private int compareStrings(final String segment1, final String segment2, final boolean numeric1,
                             final boolean numeric2) {
    int stringCompare = segment1.compareTo(segment2);
    if (stringCompare == 0) {
      return stringCompare;
    } else {
      if (numeric1 && numeric2) {
        return new BigInteger(segment1).compareTo(new BigInteger(segment2));
      } else {
        return stringCompare;
      }
    }
  }
}
