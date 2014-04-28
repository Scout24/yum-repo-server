package de.is24.infrastructure.gridfs.http.utils;

import org.bouncycastle.util.IPAddress;


public class HostName {
  private final String name;
  private final String shortName;
  private final boolean isIp;

  public HostName(String name) {
    if (name == null) {
      throw new IllegalArgumentException("host name must not be null");
    }
    isIp = IPAddress.isValidIPv4(name) || IPAddress.isValidIPv6(name);
    this.name = name;
    if (!isIp) {
      this.shortName = name.split("\\.")[0];
    } else {
      this.shortName = null;
    }
  }


  public String getName() {
    return name;
  }

  public String getShortName() {
    return shortName;
  }

  public boolean isIp() {
    return isIp;
  }

  @Override
  public String toString() {
    return name;
  }
}
