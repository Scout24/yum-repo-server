package de.is24.infrastructure.gridfs.http.utils;

public class HostName {
  public HostName(String name) {
    if (name == null) {
      throw new IllegalArgumentException("host name must not be null");
    }
    isIp = isIPAddress(name);
    this.name = name;
    if (!isIp) {
      int dotIndex = name.indexOf(".");
      if (dotIndex >= 0) {
        this.shortName = name.substring(0, dotIndex);
      } else {
        this.shortName = name;
      }
    } else {
      this.shortName = null;
    }
  }

  private final String name;
  private final String shortName;
  private final boolean isIp;

  public static boolean isIPAddress(String hostnameOrIP) {
    return hostnameOrIP.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
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
