package de.is24.infrastructure.gridfs.http.domain;

import de.is24.infrastructure.gridfs.http.domain.yum.YumPackage;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.YUM_ENTRY_COLLECTION;

@Document(collection = YUM_ENTRY_COLLECTION)
public class YumEntry {

  @Id
  private ObjectId id;
  private YumPackage yumPackage;
  @Indexed
  private String repo;

  public YumEntry(ObjectId id, String repo, YumPackage yumPackage) {
    this.id = id;
    this.repo = repo;
    this.yumPackage = yumPackage;
  }

  public String getRepo() {
    return repo;
  }

  public void setRepo(String repo) {
    this.repo = repo;
  }

  public YumPackage getYumPackage() {
    return yumPackage;
  }

  public void setYumPackage(YumPackage yumPackage) {
    this.yumPackage = yumPackage;
  }

  public ObjectId getId() {
    return id;
  }

  public void setId(ObjectId id) {
    this.id = id;
  }
}

