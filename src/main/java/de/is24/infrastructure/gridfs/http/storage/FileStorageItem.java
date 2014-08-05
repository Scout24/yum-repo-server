package de.is24.infrastructure.gridfs.http.storage;

import java.io.InputStream;
import java.util.Date;

public interface FileStorageItem {
  boolean isMarkedAsDeleted();

  String getRepo();

  Object getId();

  String getFilename();

  InputStream getInputStream();

  long getSize();

  String getChecksumSha256();

  Date getUploadDate();

  String getContentType();

  Date getDateOfMarkAsDeleted();

  String getArch();
}
