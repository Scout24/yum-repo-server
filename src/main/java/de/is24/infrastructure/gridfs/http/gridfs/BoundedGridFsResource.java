package de.is24.infrastructure.gridfs.http.gridfs;

import de.is24.infrastructure.gridfs.http.storage.FileStorageItem;
import org.apache.commons.io.input.BoundedInputStream;
import org.springframework.core.io.InputStreamResource;

import java.io.IOException;
import java.io.InputStream;

import static java.lang.Math.min;

public class BoundedGridFsResource extends InputStreamResource {
  private final FileStorageItem storageItem;
  private final long length;
  private final long startPos;

  public BoundedGridFsResource(FileStorageItem storageItem, long startPos) throws IOException {
    super(skip(storageItem.getInputStream(), startPos));
    this.storageItem = storageItem;
    this.startPos = startPos;
    this.length = storageItem.getSize() - startPos;
  }

  public BoundedGridFsResource(FileStorageItem storageItem, long startPos, long length) throws IOException {
    super(new BoundedInputStream(skip(storageItem.getInputStream(), startPos), length));
    this.storageItem = storageItem;
    this.length = length;
    this.startPos = startPos;
  }

  @Override
  public long contentLength() throws IOException {
    return min(storageItem.getSize() - startPos, length);
  }

  @Override
  public String getFilename() throws IllegalStateException {
    return storageItem.getFilename();
  }

  @Override
  public long lastModified() throws IOException {
    return storageItem.getUploadDate().getTime();
  }

  public long getFileLength() {
    return storageItem.getSize();
  }

  public String getContentType() {
    return storageItem.getContentType();
  }

  private static InputStream skip(InputStream inputStream, long startPos) throws IOException {
    inputStream.skip(startPos);
    return inputStream;
  }
}
