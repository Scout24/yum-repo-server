package de.is24.infrastructure.gridfs.http.web.domain;

import de.is24.infrastructure.gridfs.http.domain.RepoEntry;

import java.util.Date;

public class RepositoryInfo {

  private final boolean needsMetadataUpdate;
  private final Date lastMetadataGeneration;
  private final String hashOfEntries;

  public RepositoryInfo(RepoEntry repoEntry, boolean needsMetadataUpdate) {
    this.needsMetadataUpdate = needsMetadataUpdate;
    this.lastMetadataGeneration =  repoEntry.getLastMetadataGeneration();

    this.hashOfEntries = repoEntry.getHashOfEntries();
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
}
