package de.is24.infrastructure.gridfs.http.storage;


import de.is24.infrastructure.gridfs.http.gridfs.GridFsFileDescriptor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

public interface FileStorageService {
  public FileStorageItem findById(Object id);

  public FileStorageItem findBy(GridFsFileDescriptor descriptor);

  public void delete(FileStorageItem storageItem);

  public void moveTo(FileStorageItem storageItem, String destinationRepo);

  public List<FileStorageItem> getAllRpms(String repo);

  public FileStorageItem storeFile(InputStream inputStream, GridFsFileDescriptor descriptor);

  public UploadResult storeSqliteFileCompressedWithChecksumName(String reponame, File metadataFile, String name) throws IOException;

  public List<FileStorageItem> getAllRpms();

  public void removeFilesMarkedAsDeletedBefore(final Date before);
}
