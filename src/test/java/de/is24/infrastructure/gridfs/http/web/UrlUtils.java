package de.is24.infrastructure.gridfs.http.web;

import java.net.URL;

public class UrlUtils {

  public static String join(URL pathA, String pathB) {
    if (pathA.getPath().endsWith("/") && pathB.startsWith("/")) {
      return pathA + pathB.substring(1);
    }

    return pathA + pathB;
  }
}
