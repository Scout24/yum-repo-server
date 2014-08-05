package de.is24.infrastructure.gridfs.http.gridfs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mongodb.gridfs.GridFSDBFile;
import de.is24.infrastructure.gridfs.http.storage.FileStorageItem;
import org.springframework.util.Assert;

import java.io.InputStream;
import java.util.Date;

import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.ARCH_KEY;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.MARKED_AS_DELETED_KEY;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.REPO_KEY;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.SHA256_KEY;

public class GridFsFileStorageItem implements FileStorageItem {
  private final GridFSDBFile dbFile;

  public GridFsFileStorageItem(GridFSDBFile dbFile) {
    Assert.notNull(dbFile);
    this.dbFile = dbFile;
  }

  @Override
  public boolean isMarkedAsDeleted() {
    return getDateOfMarkAsDeleted() != null;
  }

  @Override
  public String getRepo() {
    return (String) dbFile.getMetaData().get(REPO_KEY);
  }

  @Override
  public Object getId() {
    return dbFile.getId();
  }

  @JsonIgnore
  @Override
  public String getFilename() {
    return dbFile.getFilename();
  }

  @Override
  public InputStream getInputStream() {
    return dbFile.getInputStream();
  }

  @Override
  public long getSize() {
    return dbFile.getLength();
  }

  @Override
  public String getChecksumSha256() {
    return dbFile.getMetaData().get(SHA256_KEY).toString();
  }

  @Override
  public Date getUploadDate() {
    return dbFile.getUploadDate();
  }

  @Override
  public String getContentType() {
    return dbFile.getContentType();
  }

  @Override
  public Date getDateOfMarkAsDeleted() {
    return (Date) dbFile.getMetaData().get(MARKED_AS_DELETED_KEY);
  }

  @Override
  public String getArch() {
    return (String) dbFile.getMetaData().get(ARCH_KEY);
  }

  @JsonIgnore
  public GridFSDBFile getDbFile() {
    return dbFile;
  }
}
