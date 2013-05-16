package de.is24.infrastructure.gridfs.http.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Set;
import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;


public class Container<T extends SizeProvider> {
  private String path;
  private long totalSize;
  private Set<T> items;
  private boolean showInfo = false;

  Container() {
  }

  public Container(String path, Container<T> container) {
    this(path, container.getItems());
    this.setShowInfo(container.isShowInfo());
  }

  public Container(String path, Set<T> items) {
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

  public Set<T> getItems() {
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
