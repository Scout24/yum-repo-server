package de.is24.infrastructure.gridfs.http.web.boot;

import org.springframework.test.annotation.ProfileValueSource;

public class LocalConfig implements ProfileValueSource {

  public static final String LOCAL_KEY = "local";
  public static final String REMOTE_KEY = "remote";
  public static final String TRUE = "true";
  public static final String FALSE = "false";
  public static final String REMOTE_CONTAINER_URL_KEY = "remote.container.url";

  public static boolean isRemoteEnabled() {
    return System.getProperty(REMOTE_CONTAINER_URL_KEY) != null;
  }

  @Override
  public String get(String key) {
    if ("local".equals(key)) {
      return Boolean.toString(!isRemoteEnabled());
    }
    if ("remote".equals(key)) {
      return Boolean.toString(isRemoteEnabled());
    }
    return null;
  }


}
