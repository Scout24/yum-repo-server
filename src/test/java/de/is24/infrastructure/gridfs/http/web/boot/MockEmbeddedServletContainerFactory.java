package de.is24.infrastructure.gridfs.http.web.boot;

import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.ServletContextInitializer;

public class MockEmbeddedServletContainerFactory implements EmbeddedServletContainerFactory {
  @Override
  public EmbeddedServletContainer getEmbeddedServletContainer(ServletContextInitializer... initializers) {
    return null;
  }
}
