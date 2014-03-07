package de.is24.infrastructure.gridfs.http.security;


import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IpRangeTest {

  @Test
  public void inRange() throws Exception {
    in("10.10.10.10", "10.10.10.10");
    in("10.10.10.10", "10.10.10.1-20");
    in("10.10.10.10", "10.10.10.1-10");
    in("10.10.10.10", "10.10.10.10-255");
    in("10.10.10.10", "10.5-15.10.1-20");
  }

  @Test
  public void notInRange() throws Exception {
    notIn("10.10.10.10", "10.10.12.10");
    notIn("10.10.10.10", "10.10.10.1-9");
    notIn("10.10.10.10", "10.10.10.11-255");
    notIn("10.10.10.10", "10.20-255.10.10");
    notIn("10.10.10.10", "10.5-15.10.20-30");
    notIn("10.10.10.10", "10.5-15.10.20");
  }

  private void notIn(String ip, String range) {
    assertFalse(new IpRange(range).isIn(ip));
  }

  private void in(String ip, String range) {
    assertTrue(new IpRange(range).isIn(ip));
  }
}
