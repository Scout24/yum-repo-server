package de.is24.infrastructure.gridfs.http.storage;

import java.io.InputStream;
import java.util.Date;

public interface FileStorageItem {
  public boolean isMarkedAsDeleted();

  public String getRepo();

  public Object getId();

  public String getFilename();

  public InputStream getInputStream();

  public long getSize();

  public String getChecksumSha256();

  public Date getUploadDate();

  public String getContentType();

  public Date getDateOfMarkAsDeleted();

  public String getArch();
}
