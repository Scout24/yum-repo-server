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
  FileStorageItem findById(Object id);

  FileStorageItem findBy(FileDescriptor descriptor);

  @PreAuthorize(HAS_DESCRIPTOR_READ_PERMISSION)
  @TimeMeasurement
  FileStorageItem getFileBy(FileDescriptor descriptor);

  void delete(FileStorageItem storageItem);

  void delete(FileDescriptor descriptor);

  void moveTo(FileStorageItem storageItem, String destinationRepo);

  List<FileStorageItem> getAllRpms(String repo);

  FileStorageItem storeFile(InputStream inputStream, FileDescriptor descriptor);

  FileStorageItem storeFile(InputStream inputStream, FileDescriptor descriptor, boolean allowOverride);

  UploadResult storeSqliteFileCompressedWithChecksumName(String reponame, File metadataFile, String name)
                                                  throws IOException;

  List<FileStorageItem> getAllRpms();

  void removeFilesMarkedAsDeletedBefore(final Date before);

  void markForDeletionByPath(final String path);

  void markForDeletionByFilenameRegex(final String regex);

  void deleteRepo(String reponame);

  List<FileStorageItem> findByPrefix(String prefix);

  void setUploadDate(FileStorageItem file, Date date);

  @PreAuthorize(HAS_DESCRIPTOR_READ_PERMISSION)
  BoundedGridFsResource getResource(FileDescriptor descriptor) throws IOException;

  @PreAuthorize(HAS_DESCRIPTOR_READ_PERMISSION)
  BoundedGridFsResource getResource(FileDescriptor descriptor, long startPos) throws IOException;

  @PreAuthorize(HAS_DESCRIPTOR_READ_PERMISSION)
  BoundedGridFsResource getResource(FileDescriptor descriptor, long startPos, long size) throws IOException;

  List<FileStorageItem> getCorruptFiles();

  void deleteCorruptFiles();
}
