package org.jboss.arquillian.mongo;

import org.jboss.arquillian.test.api.ArquillianResource;

import java.lang.annotation.Annotation;
import java.net.URL;

public class RemoteURLResourceProvider implements RemoteResourceProvider {
  @Override
  public boolean canProvide(Class<?> type) {
    return URL.class.equals(type);
  }

  @Override
  public Object lookup(ArquillianResource resource, URL remoteUrl, Annotation... qualifiers) {
    return remoteUrl;
  }
}
