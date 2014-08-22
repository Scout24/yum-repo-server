package de.is24.infrastructure.gridfs.http.web.boot;

public interface MongoPasswordManager {
  void setWrongPassword();

  void setCorrectPassword();
}
