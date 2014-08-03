package de.is24.infrastructure.gridfs.http.storage;

import java.util.Date;

/**
 * Created by sherold on 03.08.14.
 */
public class UploadResult {
  private Date uploadDate;
  private long compressedSize;
  private String compressedChecksum;
  private long uncompressedSize;
  private String uncompressedChecksum;
  private String location;

  public void setUploadDate(Date uploadDate) {
    this.uploadDate = uploadDate;
  }

  public Date getUploadDate() {
    return uploadDate;
  }

  public void setCompressedSize(long compressedSize) {
    this.compressedSize = compressedSize;
  }

  public long getCompressedSize() {
    return compressedSize;
  }

  public void setCompressedChecksum(String compressedChecksum) {
    this.compressedChecksum = compressedChecksum;
  }

  public String getCompressedChecksum() {
    return compressedChecksum;
  }

  public void setUncompressedSize(long uncompressedSize) {
    this.uncompressedSize = uncompressedSize;
  }

  public long getUncompressedSize() {
    return uncompressedSize;
  }

  public void setUncompressedChecksum(String uncompressedChecksum) {
    this.uncompressedChecksum = uncompressedChecksum;
  }

  public String getUncompressedChecksum() {
    return uncompressedChecksum;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getLocation() {
    return location;
  }
}
