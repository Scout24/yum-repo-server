package de.is24.infrastructure.gridfs.http.storage;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

public interface FileStorageService {
  public FileStorageItem findById(Object id);

  public FileStorageItem findBy(FileDescriptor descriptor);

  public void delete(FileStorageItem storageItem);

  public void moveTo(FileStorageItem storageItem, String destinationRepo);

  public List<FileStorageItem> getAllRpms(String repo);

  public FileStorageItem storeFile(InputStream inputStream, FileDescriptor descriptor);

  public FileStorageItem storeFile(InputStream inputStream, FileDescriptor descriptor, boolean allowOverride);

  public UploadResult storeSqliteFileCompressedWithChecksumName(String reponame, File metadataFile, String name) throws IOException;

  public List<FileStorageItem> getAllRpms();

  public void removeFilesMarkedAsDeletedBefore(final Date before);

  public void markForDeletionByPath(final String path);

  public void markForDeletionByFilenameRegex(final String regex);

  public void deleteRepo(String reponame);
}
