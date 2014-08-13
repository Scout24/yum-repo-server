package org.jboss.arquillian.mongo;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import org.jboss.arquillian.test.api.ArquillianResource;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;

import static com.mongodb.MongoCredential.createMongoCRCredential;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.isBlank;


public class RemoteMongoResourceProvider implements RemoteResourceProvider {
  @Override
  public boolean canProvide(Class<?> type) {
    return Mongo.class.equals(type);
  }

  @Override
  public Object lookup(ArquillianResource resource, URL remoteUrl, Annotation... qualifiers) {
    try {
      List<ServerAddress> serverAddressList = asList(new ServerAddress(remoteUrl.getHost()));
      MongoCredentials mongoCredentials = getCredentials(qualifiers);
      checkCredentials(mongoCredentials);
      List<MongoCredential> credentials = asList(createMongoCRCredential(
          mongoCredentials.username(),
          mongoCredentials.db(),
          mongoCredentials.password().toCharArray()
      ));
      return new MongoClient(serverAddressList, credentials);
    } catch (UnknownHostException e) {
      throw new RuntimeException("Could not find host " + remoteUrl.getHost(), e);
    }
  }

  private MongoCredentials getCredentials(Annotation[] qualifiers) {
    if (qualifiers != null) {
      for (Annotation annotation : qualifiers) {
        if (annotation instanceof MongoCredentials) {
          return (MongoCredentials) annotation;
        }
      }
    }

    throw new IllegalArgumentException("Please specific @MongoCredentials for @ArquillianResource Mongo");
  }

  private void checkCredentials(MongoCredentials credentials) {
    if (isBlank(credentials.db())) {
      throw new IllegalArgumentException("db need to be set.");
    }
    if (isBlank(credentials.username())) {
      throw new IllegalArgumentException("username need to be set.");
    }
    if (credentials.password() == null) {
      throw new IllegalArgumentException("password have not to be null");
    }
  }
}
