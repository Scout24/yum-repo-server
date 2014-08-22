package de.is24.infrastructure.gridfs.http.mongo.util;

import com.mongodb.BasicDBList;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;

import static com.mongodb.BasicDBObjectBuilder.start;


public class MongoProcessHolder {
  private static final Logger LOGGER = LoggerFactory.getLogger(MongoProcessHolder.class);
  public static final String MONGO_USERNAME = "reposerver";
  public static final String MONGO_PASSWORD = "reposerver";
  public static final String MONGO_DB_NAME = "rpm_db";
  private static final String WRONG_PASSWORD = "foobar123";

  private final MongodExecutable mongodExecutable;
  private final MongodProcess mongodProcess;
  private final int mongoPort;
  private final Mongo adminMongo;

  public MongoProcessHolder(MongodExecutable mongodExecutable, MongodProcess mongodProcess, int mongoPort) {
    this.mongodExecutable = mongodExecutable;
    this.mongodProcess = mongodProcess;
    this.mongoPort = mongoPort;
    try {
      this.adminMongo = new MongoClient("localhost", mongoPort);
    } catch (UnknownHostException e) {
      throw new IllegalArgumentException("Could not resolve MongoDB host", e);
    }

    updateDbUser(true, MONGO_PASSWORD);
    setSystemProperties();
  }

  public void setSystemProperties() {
    System.setProperty("mongodb.port", "" + mongoPort);
    System.setProperty("mongodb.serverlist", "localhost");
    System.setProperty("mongodb.db.user", MONGO_USERNAME);
    System.setProperty("mongodb.db.pass", MONGO_PASSWORD);
  }

  public int getMongoPort() {
    return mongoPort;
  }

  public void stopMongo() {
    LOGGER.info("try to stop Mongo process for port {}", mongoPort);
    try {
      mongodProcess.stop();
    } catch (Exception e) {
      LOGGER.warn("Could not stop mongodProcess. Did it start? " + mongodProcess, e);
    }
    try {
      mongodExecutable.stop();
    } catch (Exception e) {
      LOGGER.warn("Could not stop mongodExecutable. Did it start? " + mongodExecutable, e);
    }
    LOGGER.info("stopped Mongo process for port {}", mongoPort);
  }

  public void setWrongPasswordAndDropDatabase() {
    updateDbUser(false, WRONG_PASSWORD);
    adminMongo.dropDatabase(MONGO_DB_NAME);
  }

  public void setCorrectPassword() {
    updateDbUser(false, MONGO_PASSWORD);
  }

  private void updateDbUser(boolean create, String password) {
    DB db = adminMongo.getDB(MONGO_DB_NAME);
    BasicDBList roles = new BasicDBList();
    roles.add("dbAdmin");
    db.command(start(create ? "createUser" : "updateUser", MONGO_USERNAME)
        .add("pwd", password)
        .add("roles", roles).get());
  }
}
