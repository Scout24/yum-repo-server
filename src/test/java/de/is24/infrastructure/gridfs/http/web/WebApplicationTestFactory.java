package de.is24.infrastructure.gridfs.http.web;

import com.mongodb.FastestPingTimeReadPreference;
import org.jboss.arquillian.util.WebArchiveBuilder;
import org.jboss.shrinkwrap.api.spec.WebArchive;


public class WebApplicationTestFactory {
  public static WebArchive createCompleteApplication() {
    return new WebArchiveBuilder("ROOT.war").withSpringWebDependencies()
      .withSpringSecurityDependencies()
      .withPackagesToTest("de.is24.infrastructure.gridfs.http")
      .withClassesToTest(FastestPingTimeReadPreference.class)
      .withAllResources()
      .withTestResouce("log4j.xml")
      .build();
  }
}
