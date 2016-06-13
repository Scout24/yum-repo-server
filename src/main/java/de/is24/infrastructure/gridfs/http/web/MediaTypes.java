package de.is24.infrastructure.gridfs.http.web;

import org.springframework.http.MediaType;

public class MediaTypes {
  public static final MediaType APPLICATION_X_RPM = new MediaType("application", "x-rpm");
  public static final String CONTENT_TYPE_APPLICATION_X_RPM = APPLICATION_X_RPM.toString();

  public static final String CONTENT_TYPE_APPLICATION_X_GPG = "application/x-gpg";

  public static final String BZ2_CONTENT_TYPE = new MediaType("application", "x-bzip2").toString();
}
