package de.is24.infrastructure.gridfs.http.web.boot;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import de.is24.infrastructure.gridfs.http.PropertyConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;

import static com.mongodb.MongoCredential.createMongoCRCredential;
import static de.is24.infrastructure.gridfs.http.Profiles.REMOTE_TESTS;
import static de.is24.infrastructure.gridfs.http.mongo.util.LocalMongoFactory.MONGO_DB_NAME;
import static de.is24.infrastructure.gridfs.http.mongo.util.LocalMongoFactory.MONGO_PASSWORD;
import static de.is24.infrastructure.gridfs.http.mongo.util.LocalMongoFactory.MONGO_USERNAME;
import static java.util.Arrays.asList;

@Import(PropertyConfig.class)
@Profile(REMOTE_TESTS)
public class SpringBootRemoteAppConfig {

  @Value("${remote.container.url}")
  URL remoteContainerUrl;

  @Autowired
  Environment environment;

  @Bean
  public Mongo mongo() throws UnknownHostException {
    List<ServerAddress> serverAddressList = asList(new ServerAddress(remoteContainerUrl.getHost()));
    List<MongoCredential> credentials = asList(createMongoCRCredential(
        MONGO_USERNAME,
        MONGO_DB_NAME,
        MONGO_PASSWORD.toCharArray()
    ));
    return new MongoClient(serverAddressList, credentials);
  }

  @Bean
  public EmbeddedServletContainerFactory embeddedServletContainerFactory() {
    setProperty("local.deployment.url", remoteContainerUrl.toString());
    return new MockEmbeddedServletContainerFactory();
  }

  private void setProperty(String propertyName, String url) {
    if (environment instanceof ConfigurableEnvironment) {
      EnvironmentTestUtils.addEnvironment((ConfigurableEnvironment) environment, propertyName + ":" + url);
    }
  }
}
