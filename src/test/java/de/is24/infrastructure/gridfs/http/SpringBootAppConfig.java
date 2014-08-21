package de.is24.infrastructure.gridfs.http;

import de.is24.infrastructure.gridfs.http.mongo.util.LocalMongoFactory;
import de.is24.infrastructure.gridfs.http.mongo.util.MongoProcessHolder;
import de.is24.infrastructure.gridfs.http.utils.StatsdMockServer;
import de.is24.infrastructure.gridfs.http.web.WebConfig;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.annotation.PreDestroy;

@EnableAutoConfiguration
@Import({AppConfig.class, WebConfig.class})
@Configuration
public class SpringBootAppConfig {

  protected MongoProcessHolder mongoProcessHolder;
  public StatsdMockServer statsdMockServer;

  public SpringBootAppConfig() throws Throwable {
    startMongo();

    startStatsd();
  }

  public void startStatsd() throws Throwable {
    statsdMockServer = new StatsdMockServer();
    statsdMockServer.before();
    System.setProperty("statsd.host", "localhost");
    System.setProperty("statsd.port", Integer.toString(statsdMockServer.getPort()));
  }

  public void startMongo() throws Throwable {
    mongoProcessHolder = LocalMongoFactory.createMongoProcess();
  }

  @Bean
  public ServletContextInitializer addFilter() {
    return servletContext -> new AppInitializer().addAllFilters(servletContext);
  }

  @PreDestroy
  public void stopServices() {
    stopMongo();
    stopStatsd();
  }

  public void stopStatsd() {
    if (statsdMockServer != null) {
      statsdMockServer.after();
    }
  }

  public void stopMongo() {
    if (mongoProcessHolder != null) {
      mongoProcessHolder.stopMongo();
    }
  }
}
