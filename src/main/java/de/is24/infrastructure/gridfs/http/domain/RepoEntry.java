package de.is24.infrastructure.gridfs.http.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import static de.is24.infrastructure.gridfs.http.domain.RepoType.VIRTUAL;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.REPO_ENTRY_COLLECTION;
import static org.apache.commons.lang.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang.builder.HashCodeBuilder.reflectionHashCode;


@Document(collection = REPO_ENTRY_COLLECTION)
public class RepoEntry {
  @Id
  private ObjectId id;

  @Indexed(unique = true, sparse = true)
  private String name;

  public static final int DEFAULT_MAX_KEEP_RPMS = 1;

  private RepoType type;

  private Date lastModified;

  private Date lastMetadataGeneration;

  private boolean external;

  private String target;

  private boolean undeletable;

  private Set<String> tags = new HashSet<>();

  private int maxKeepRpms = DEFAULT_MAX_KEEP_RPMS;

  private String hashOfEntries;

  public ObjectId getId() {
    return id;
  }

  public void setId(ObjectId id) {
    this.id = id;
  }

  public RepoType getType() {
    return type;
  }

  public void setType(RepoType type) {
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Date getLastModified() {
    return lastModified;
  }

  public void setLastModified(Date lastModified) {
    this.lastModified = lastModified;
  }

  public Date getLastMetadataGeneration() {
    return lastMetadataGeneration;
  }

  public void setLastMetadataGeneration(Date lastMetadataGeneration) {
    this.lastMetadataGeneration = lastMetadataGeneration;
  }

  public boolean isExternal() {
    return external;
  }

  @JsonIgnore
  public boolean isVirtualInternal() {
    return !isExternal() && (type == VIRTUAL);
  }

  public void setExternal(boolean external) {
    this.external = external;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  @Override
  public boolean equals(Object obj) {
    return reflectionEquals(this, obj);
  }

  @Override
  public int hashCode() {
    return reflectionHashCode(this);
  }

  public void setUndeletable(boolean undeletable) {
    this.undeletable = undeletable;
  }

  public boolean isUndeletable() {
    return undeletable;
  }

  public Set<String> getTags() {
    return tags;
  }

  public void setTags(Set<String> tags) {
    this.tags = tags;
  }

  public int getMaxKeepRpms() {
    return maxKeepRpms;
  }

  public void setMaxKeepRpms(int maxKeepRpms) {
    this.maxKeepRpms = maxKeepRpms;
  }

  public String getHashOfEntries() {
    return hashOfEntries;
  }

  public void setHashOfEntries(String hashOfEntries) {
    this.hashOfEntries = hashOfEntries;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).append("id", id)
      .append("name", name)
      .append("type", type)
      .append("lastModified", lastModified)
      .append("lastMetadataGeneration", lastMetadataGeneration)
      .append("external", external)
      .append("target", target)
      .append("undeletable", undeletable)
      .append("tags", tags)
      .append("maxKeepRpms", maxKeepRpms)
      .append("hashOfEntries", hashOfEntries)
      .toString();
  }
}
