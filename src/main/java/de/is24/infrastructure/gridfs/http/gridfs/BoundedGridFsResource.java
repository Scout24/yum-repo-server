package de.is24.infrastructure.gridfs.http.gridfs;

import com.mongodb.gridfs.GridFSDBFile;
import org.apache.commons.io.input.BoundedInputStream;
import org.springframework.core.io.InputStreamResource;

import java.io.IOException;
import java.io.InputStream;

import static java.lang.Math.min;

public class BoundedGridFsResource extends InputStreamResource {
  private final GridFSDBFile file;
  private final long length;
  private final long startPos;

  public BoundedGridFsResource(GridFSDBFile file, long startPos) throws IOException {
    super(skip(file.getInputStream(), startPos));
    this.file = file;
    this.startPos = startPos;
    this.length = file.getLength() - startPos;
  }

  public BoundedGridFsResource(GridFSDBFile file, long startPos, long length) throws IOException {
    super(new BoundedInputStream(skip(file.getInputStream(), startPos), length));
    this.file = file;
    this.length = length;
    this.startPos = startPos;
  }

  @Override
  public long contentLength() throws IOException {
    return min(file.getLength() - startPos, length);
  }

  @Override
  public String getFilename() throws IllegalStateException {
    return file.getFilename();
  }

  @Override
  public long lastModified() throws IOException {
    return file.getUploadDate().getTime();
  }

  public long getFileLength() {
    return file.getLength();
  }

  public String getContentType() {
    return file.getContentType();
  }

  private static InputStream skip(InputStream inputStream, long startPos) throws IOException {
    inputStream.skip(startPos);
    return inputStream;
  }
}
