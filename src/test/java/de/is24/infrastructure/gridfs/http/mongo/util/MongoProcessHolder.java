package de.is24.infrastructure.gridfs.http.mongo.util;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MongoProcessHolder {
  private static final Logger LOGGER = LoggerFactory.getLogger(MongoProcessHolder.class);

  private final MongodExecutable mongodExecutable;
  private final MongodProcess mongodProcess;

  private final int mongoPort;

  public MongoProcessHolder(MongodExecutable mongodExecutable, MongodProcess mongodProcess, int mongoPort) {
    this.mongodExecutable = mongodExecutable;
    this.mongodProcess = mongodProcess;
    this.mongoPort = mongoPort;
  }

  public MongodExecutable getMongodExecutable() {
    return mongodExecutable;
  }


  public MongodProcess getMongodProcess() {
    return mongodProcess;
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

}
