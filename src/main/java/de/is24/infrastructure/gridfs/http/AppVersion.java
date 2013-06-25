package de.is24.infrastructure.gridfs.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class AppVersion {
  private final String version;

  @Autowired
  public AppVersion(@Value("${application.version:notSet}") final String version) {
    this.version = version;
  }

  public String getVersion() {
    return version;
  }
}
