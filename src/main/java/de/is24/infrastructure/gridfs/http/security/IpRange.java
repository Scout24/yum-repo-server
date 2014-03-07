package de.is24.infrastructure.gridfs.http.security;

import org.springframework.util.Assert;

import static java.lang.Integer.parseInt;

/**
 * Class to support simple ip address ranges like:
 * <ul>
 *   <li><code>10.10.10.10</code> plain ip addresses</li>
 *   <li><code>10.10.10.10-12</code> simple ranges in one component</li>
 *   <li><code>10.5-15.10.10-12</code> ip ranges in multiple components</li>
 *   <li><code>10.5-15.0-255.0-255</code> whole (sub-)network</li>
 * </ul>
 */
public class IpRange {

  public static final int IP_ADDRESS_BYTE_COUNT = 4;
  private final ByteRange[] byteRanges = new ByteRange[IP_ADDRESS_BYTE_COUNT];

  public IpRange(String range) {
    Assert.hasText(range, "Please provide a valid IP range");
    String[] split = range.split("\\.");
    Assert.isTrue(split.length == IP_ADDRESS_BYTE_COUNT, "Ip ranges need 4 segments. Was: " + range);
    for (int i = 0; i < IP_ADDRESS_BYTE_COUNT; i++) {
      byteRanges[i] = new ByteRange(split[i]);
    }
  }

  public boolean isIn(String ip) {
    Assert.hasText(ip, "Please provide a valid IP address");
    String[] split = ip.split("\\.");
    Assert.isTrue(split.length == IP_ADDRESS_BYTE_COUNT, "Ip addresses need 4 segments. Was: " + ip);
    for (int i = 0; i < IP_ADDRESS_BYTE_COUNT; i++) {
      if (!byteRanges[i].isIn(parseInt(split[i]))) {
        return false;
      }
    }

    return true;
  }

  static class ByteRange {

    private final int min;
    private final int max;

    ByteRange(String range) {
      int pos = range.indexOf('-');
      if (pos < 0) {
        min = parseInt(range);
        max = min;
      } else {
        min = parseInt(range.substring(0, pos));
        max = parseInt(range.substring(pos + 1, range.length()));
      }

      checkInterval(min);
      checkInterval(max);
      checkOrder(min, max);
    }

    private void checkOrder(int min, int max) {
      if (min > max) {
        throw new IllegalArgumentException("Range start needs to be less or equal range end. Was: [" + min + "," + max +"]");
      }
    }

    private void checkInterval(int ipSegment) {
      if (ipSegment < 0 || ipSegment > 255) {
        throw new IllegalArgumentException("Ip segement needs to be in 0-255. Was: " + ipSegment);
      }
    }

    public boolean isIn(int num) {
      return num >= min && num <= max;
    }
  }
}
