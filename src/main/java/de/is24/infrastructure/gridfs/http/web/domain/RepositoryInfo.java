package de.is24.infrastructure.gridfs.http.web.domain;

import de.is24.infrastructure.gridfs.http.domain.RepoEntry;
import de.is24.infrastructure.gridfs.http.domain.RepoType;

import java.util.Date;

public class RepositoryInfo {

  private final boolean needsMetadataUpdate;
  private final Date lastMetadataGeneration;
  private final String hashOfEntries;
  private final RepoType type;

  public RepositoryInfo(RepoEntry repoEntry, boolean needsMetadataUpdate) {
    this.needsMetadataUpdate = needsMetadataUpdate;
    this.lastMetadataGeneration =  repoEntry.getLastMetadataGeneration();
    this.hashOfEntries = repoEntry.getHashOfEntries();
    this.type = repoEntry.getType();
  }

  public boolean isNeedsMetadataUpdate() {
    return needsMetadataUpdate;
  }

  public Date getLastMetadataGeneration() {
    return lastMetadataGeneration;
  }

  public String getHashOfEntries() {
    return hashOfEntries;
  }

  public RepoType getType() {
    return type;
  }
}
