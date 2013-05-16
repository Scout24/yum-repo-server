package org.jboss.arquillian.mongo;

import org.jboss.arquillian.test.api.ArquillianResource;

import java.lang.annotation.Annotation;
import java.net.URL;

public interface RemoteResourceProvider {

  public boolean canProvide(Class<?> type);

  public Object lookup(ArquillianResource resource, URL remoteUrl, Annotation... qualifiers);
}
