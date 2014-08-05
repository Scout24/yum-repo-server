package de.is24.infrastructure.gridfs.http.storage;


import de.is24.infrastructure.gridfs.http.gridfs.BoundedGridFsResource;
import de.is24.util.monitoring.spring.TimeMeasurement;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import static de.is24.infrastructure.gridfs.http.security.Permission.HAS_DESCRIPTOR_READ_PERMISSION;

public interface FileStorageService {
  public FileStorageItem findById(Object id);

  public FileStorageItem findBy(FileDescriptor descriptor);

  @TimeMeasurement
  FileStorageItem insecureFindBy(FileDescriptor descriptor);

  @TimeMeasurement
  @PreAuthorize(HAS_DESCRIPTOR_READ_PERMISSION)
  FileStorageItem getFileBy(FileDescriptor descriptor);

  public void delete(FileStorageItem storageItem);

  public void delete(FileDescriptor descriptor);

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

  public List<FileStorageItem> findByPrefix(String prefix);

  public void setUploadDate(FileStorageItem file, Date date);

  @PreAuthorize(HAS_DESCRIPTOR_READ_PERMISSION)
  BoundedGridFsResource getResource(FileDescriptor descriptor) throws IOException;

  @PreAuthorize(HAS_DESCRIPTOR_READ_PERMISSION)
  BoundedGridFsResource getResource(FileDescriptor descriptor, long startPos) throws IOException;

  @PreAuthorize(HAS_DESCRIPTOR_READ_PERMISSION)
  BoundedGridFsResource getResource(FileDescriptor descriptor, long startPos, long size)
      throws IOException;
}
