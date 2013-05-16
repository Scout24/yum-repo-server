package org.jboss.arquillian.test.impl.enricher.resource;

import org.jboss.arquillian.mongo.RemoteMongoResourceProvider;
import org.jboss.arquillian.mongo.RemoteResourceProvider;
import org.jboss.arquillian.mongo.RemoteURLResourceProvider;
import org.jboss.arquillian.test.api.ArquillianResource;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.List;

import static java.util.Arrays.asList;
import static org.jboss.arquillian.test.impl.enricher.resource.SecurityActions.getFieldsWithAnnotation;

public class RemoteResourceTestEnricher {

  private final URL remoteUrl;

  private static final List<RemoteResourceProvider> REMOTE_RESOURCE_PROVIDERS = asList(
      new RemoteURLResourceProvider(),
      new RemoteMongoResourceProvider()
  );

  public RemoteResourceTestEnricher(URL remoteUrl) {
    this.remoteUrl = remoteUrl;
  }

  public void enrich(Object testCase) {
    for (Field field : getFieldsWithAnnotation(testCase.getClass(), ArquillianResource.class)) {
      RemoteResourceProvider remoteResourceProvider = getRemoteResourceProvider(field.getType());
      try {
        if (!field.isAccessible()) {
          field.setAccessible(true);
        }
        ArquillianResource arquillianResource = field.getAnnotation(ArquillianResource.class);
        field.set(testCase, remoteResourceProvider.lookup(arquillianResource, remoteUrl, field.getAnnotations()));
      } catch (Exception e) {
        throw new RuntimeException("Could not set URL on field " + field + " using " + remoteUrl);
      }
    }
  }

  private RemoteResourceProvider getRemoteResourceProvider(Class<?> type) {
    for (RemoteResourceProvider remoteResourceProvider : REMOTE_RESOURCE_PROVIDERS) {
      if (remoteResourceProvider.canProvide(type)) {
        return remoteResourceProvider;
      }
    }

    throw new RuntimeException("Could not find RemoteResourceProvider for type: " + type);
  }
}
