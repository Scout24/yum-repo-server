package de.is24.infrastructure.gridfs.http.security;

import de.is24.infrastructure.gridfs.http.utils.HostName;

public class AuthenticationDetails {
  private final HostName remoteHost;

  public AuthenticationDetails(HostName remoteHost) {
    this.remoteHost = remoteHost;
  }

  public HostName getRemoteHost() {
    return remoteHost;
  }
}
