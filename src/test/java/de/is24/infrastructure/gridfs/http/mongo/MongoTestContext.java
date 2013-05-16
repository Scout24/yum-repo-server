package de.is24.infrastructure.gridfs.http.mongo;

import com.mongodb.Mongo;
import de.is24.infrastructure.gridfs.http.mongo.util.LocalMongoFactory;
import de.is24.infrastructure.gridfs.http.mongo.util.MongoProcessHolder;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.net.UnknownHostException;

public class MongoTestContext implements TestRule {

  private MongoProcessHolder mongoProcessHolder;

  @Override
  public Statement apply(final Statement baseStatement, Description description) {
    return new Statement() {

      @Override
      public void evaluate() throws Throwable {
        mongoProcessHolder = LocalMongoFactory.createMongoProcess();
        try {
          baseStatement.evaluate();
        } finally {
          stopMongo();
        }
      }
    };
  }

  public void stopMongo() {
    checkMongoIsRunning();
    mongoProcessHolder.stopMongo();
    mongoProcessHolder = null;
  }

  private void checkMongoIsRunning() {
    if (mongoProcessHolder == null) {
      throw new IllegalStateException("MongoDB need to be setup first before it could be shut down.");
    }
  }

  public int getPort() {
    checkMongoIsRunning();
    return mongoProcessHolder.getMongoPort();
  }

  public Mongo getMongo()  {
    checkMongoIsRunning();
    try {
      return new Mongo("localhost", getPort());
    } catch (UnknownHostException e) {
      throw new IllegalStateException("Could not resolve 'localhost'", e);
    }
  }
}
