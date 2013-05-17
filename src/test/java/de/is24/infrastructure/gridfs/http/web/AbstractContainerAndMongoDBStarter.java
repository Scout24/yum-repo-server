package de.is24.infrastructure.gridfs.http.web;

import com.mongodb.Mongo;
import de.is24.infrastructure.gridfs.http.Profiles;
import de.is24.infrastructure.gridfs.http.mongo.util.LocalMongoFactory;
import de.is24.infrastructure.gridfs.http.mongo.util.MongoProcessHolder;
import de.is24.infrastructure.gridfs.http.utils.StatsdMockServer;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OverProtocol;
import org.jboss.arquillian.mongo.MongoCredentials;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URL;
import static de.is24.infrastructure.gridfs.http.mongo.IntegrationTestContext.RPM_DB;
import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.newDefaultHttpClientWithCredentials;
import static org.springframework.core.env.AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME;


public abstract class AbstractContainerAndMongoDBStarter {
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractContainerAndMongoDBStarter.class);

  @ArquillianResource
  protected URL deploymentURL;

  public static final String MONGO_USERNAME = "reposerver";
  public static final String MONGO_PASSWORD = "reposerver";

  @ArquillianResource(AbstractContainerAndMongoDBStarter.class)
  @MongoCredentials(db = RPM_DB, username = MONGO_USERNAME, password = MONGO_PASSWORD)
  protected Mongo mongo;

  protected static MongoProcessHolder mongoProcessHolder;

  public static StatsdMockServer statsdMockServer;

  protected DefaultHttpClient httpClient;

  @Before
  public void setUpHttpClient() throws Exception {
    httpClient = newDefaultHttpClientWithCredentials();
  }

  @Deployment(testable = false, managed = true)
  @OverProtocol("Servlet 3.0")
  public static WebArchive createDeployment() throws Throwable {
    startMongo();
    startStatsd();

    WebArchive webArchive = WebApplicationTestFactory.createCompleteApplication();
    System.setProperty(ACTIVE_PROFILES_PROPERTY_NAME, Profiles.DEV);
    LOGGER.debug(webArchive.toString(true));
    return webArchive;
  }

  @AfterClass
  public static void stopMongo() {
    if (mongoProcessHolder != null) {
      mongoProcessHolder.stopMongo();
    }

    if (statsdMockServer != null) {
      statsdMockServer.after();
    }
  }

  public static void startStatsd() throws Throwable {
    statsdMockServer = new StatsdMockServer();
    statsdMockServer.before();
    System.setProperty("statsd.host", "localhost");
    System.setProperty("statsd.port", Integer.toString(statsdMockServer.getPort()));
  }

  public static void startMongo() throws Throwable {
    mongoProcessHolder = LocalMongoFactory.createMongoProcess();
    System.setProperty("mongodb.port", "" + mongoProcessHolder.getMongoPort());
  }

}
