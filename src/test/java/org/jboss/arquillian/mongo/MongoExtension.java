package org.jboss.arquillian.mongo;

import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

public class MongoExtension implements LoadableExtension {
  @Override
  public void register(ExtensionBuilder builder) {
    builder.service(ResourceProvider.class, LocalMongoResourceProvider.class);
  }
}
