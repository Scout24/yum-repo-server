package de.is24.infrastructure.gridfs.http.web;

import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

public class DeploymentUrlApplicationContextInitializer implements
    ApplicationContextInitializer<ConfigurableApplicationContext> {
  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    applicationContext.addApplicationListener(new ApplicationListener<EmbeddedServletContainerInitializedEvent>() {
      @Override
      public void onApplicationEvent(
          EmbeddedServletContainerInitializedEvent event) {
        DeploymentUrlApplicationContextInitializer.this
            .onApplicationEvent(event);
      }
    });
  }

  private void onApplicationEvent(EmbeddedServletContainerInitializedEvent event) {
    setPortProperty(event.getApplicationContext(), "local.deployment.url", getUrl(event
        .getEmbeddedServletContainer().getPort()));
  }

  private String getUrl(int port) {
    return "http://localhost:" + port;
  }

  private void setPortProperty(ApplicationContext context, String propertyName, String url) {
    if (context instanceof ConfigurableApplicationContext) {
      EnvironmentTestUtils.addEnvironment((ConfigurableApplicationContext) context,
          propertyName + ":" + url);
    }
    if (context.getParent() != null) {
      setPortProperty(context.getParent(), propertyName, url);
    }
  }
}
