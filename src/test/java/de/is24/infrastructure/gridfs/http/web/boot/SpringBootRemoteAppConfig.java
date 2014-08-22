package de.is24.infrastructure.gridfs.http.web.boot;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import de.is24.infrastructure.gridfs.http.PropertyConfig;
import de.is24.infrastructure.gridfs.http.metadata.YumEntriesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;

import static com.mongodb.MongoCredential.createMongoCRCredential;
import static de.is24.infrastructure.gridfs.http.mongo.util.MongoProcessHolder.MONGO_DB_NAME;
import static de.is24.infrastructure.gridfs.http.mongo.util.MongoProcessHolder.MONGO_PASSWORD;
import static de.is24.infrastructure.gridfs.http.mongo.util.MongoProcessHolder.MONGO_USERNAME;
import static de.is24.infrastructure.gridfs.http.web.boot.LocalConfig.REMOTE_CONTAINER_URL_KEY;
import static java.util.Arrays.asList;

@Import(PropertyConfig.class)
@ConditionalOnExpression("'${" + REMOTE_CONTAINER_URL_KEY+ ":notSet}' != 'notSet'")
@EnableMongoRepositories(basePackageClasses=YumEntriesRepository.class)
public class SpringBootRemoteAppConfig implements MongoPasswordManager {

  @Value("${"+ REMOTE_CONTAINER_URL_KEY + "}")
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
  public MongoTemplate mongoTemplate() throws UnknownHostException {
    return new MongoTemplate(new SimpleMongoDbFactory(mongo(), MONGO_DB_NAME));
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

  @Override
  public void setWrongPassword() {
    throw new UnsupportedOperationException("Setting remote password is not allowed");
  }

  @Override
  public void setCorrectPassword() {
    // do nothing
  }
}
