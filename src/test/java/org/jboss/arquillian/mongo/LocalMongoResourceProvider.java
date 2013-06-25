package org.jboss.arquillian.mongo;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import de.is24.infrastructure.gridfs.http.mongo.util.MongoProcessHolder;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import static java.lang.reflect.Modifier.isStatic;
import static java.security.AccessController.doPrivileged;


public class LocalMongoResourceProvider implements ResourceProvider {
  @Override
  public boolean canProvide(Class<?> type) {
    return Mongo.class.equals(type);
  }

  @Override
  public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
    Class<?> classWithMongoField = resource.value();
    Field mongoProcessHolderField = onlyOne(findStaticMongoProcessHolderFields(classWithMongoField));

    MongoProcessHolder mongoProcessHolder;
    try {
      mongoProcessHolderField.setAccessible(true);
      mongoProcessHolder = (MongoProcessHolder) mongoProcessHolderField.get(null);
      return new MongoClient("localhost", mongoProcessHolder.getMongoPort());
    } catch (Exception e) {
      throw new RuntimeException("Could not retrieve mongo instance.", e);
    }
  }

  private Field onlyOne(List<Field> fields) {
    if (fields.isEmpty()) {
      throw new RuntimeException("Could not find static field with type " + MongoProcessHolder.class.getName());
    }

    if (fields.size() > 1) {
      throw new RuntimeException("Found multiple static fields with type " + MongoProcessHolder.class.getName());
    }

    return fields.get(0);
  }

  private List<Field> findStaticMongoProcessHolderFields(final Class<?> clazz) {
    return doPrivileged(new PrivilegedAction<List<Field>>() {
        public List<Field> run() {
          List<Field> foundFields = new ArrayList<Field>();
          Class<?> nextSource = clazz;
          while (nextSource != Object.class) {
            for (Field field : nextSource.getDeclaredFields()) {
              if (isStatic(field.getModifiers()) && MongoProcessHolder.class.isAssignableFrom(field.getType())) {
                foundFields.add(field);
              }
            }
            nextSource = nextSource.getSuperclass();
          }
          return foundFields;
        }
      });
  }
}
