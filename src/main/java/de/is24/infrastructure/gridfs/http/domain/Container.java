package de.is24.infrastructure.gridfs.http.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;


public class Container<T extends SizeProvider> {
  private String path;
  private long totalSize;
  private List<T> items;
  private boolean showInfo = false;

  Container() {
  }

  public Container(String path, Container<T> container) {
    this(path, container.getItems());
    this.setShowInfo(container.isShowInfo());
  }

  public Container(String path, List<T> items) {
    this.path = path;
    this.totalSize = 0;
    for (SizeProvider sizeProvider : items) {
      this.totalSize += sizeProvider.getSize();
    }
    this.items = items;
  }

  public String getPath() {
    return path;
  }

  public long getTotalSize() {
    return totalSize;
  }

  @JsonIgnore
  public String getFormattedTotalSize() {
    return byteCountToDisplaySize(getTotalSize());
  }

  public List<T> getItems() {
    return items;
  }

  @JsonIgnore
  public boolean isShowInfo() {
    return showInfo;
  }

  public void setShowInfo(boolean showInfo) {
    this.showInfo = showInfo;
  }
}
