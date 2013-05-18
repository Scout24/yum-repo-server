package org.jboss.arquillian.mongo;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.UnknownHostException;
import static org.apache.commons.lang.StringUtils.isBlank;


public class RemoteMongoResourceProvider implements RemoteResourceProvider {
  @Override
  public boolean canProvide(Class<?> type) {
    return Mongo.class.equals(type);
  }

  @Override
  public Object lookup(ArquillianResource resource, URL remoteUrl, Annotation... qualifiers) {
    try {
      Mongo mongo = new MongoClient(remoteUrl.getHost());

      if (qualifiers != null) {
        for (Annotation annotation : qualifiers) {
          if (annotation instanceof MongoCredentials) {
            authenticate(mongo, (MongoCredentials) annotation);
          }
        }
      }

      return mongo;
    } catch (UnknownHostException e) {
      throw new RuntimeException("Could not find host " + remoteUrl.getHost(), e);
    }
  }

  private void authenticate(Mongo mongo, MongoCredentials credentials) {
    if (isBlank(credentials.db())) {
      throw new IllegalArgumentException("db need to be set.");
    }
    if (isBlank(credentials.username())) {
      throw new IllegalArgumentException("username need to be set.");
    }
    if (credentials.password() == null) {
      throw new IllegalArgumentException("password have not to be null");
    }

    if (!mongo.getDB(credentials.db()).authenticate(credentials.username(), credentials.password().toCharArray())) {
      throw new IllegalArgumentException("Could not authenticate.");
    }
  }
}
