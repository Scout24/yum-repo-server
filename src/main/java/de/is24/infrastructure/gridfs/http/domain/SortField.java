package de.is24.infrastructure.gridfs.http.domain;

import com.mongodb.BasicDBObject;


public enum SortField {
  name("-"),
  size("length"),
  uploadDate("uploadDate"),
  repo("metadata.repo"),
  target("target");

  private SortField(String alt) {
    this.alt = alt;
  }

  private String alt;

  public BasicDBObject sortFolder(SortOrder sortOrder) {
    String field = (this == name) ? "_id" : this.alt;
    return new BasicDBObject(field, sortOrder.value);
  }

  public BasicDBObject sortFile(SortOrder sortOrder) {
    String field = (this == name) ? "filename" : this.alt;
    return new BasicDBObject(field, sortOrder.value);
  }

}
