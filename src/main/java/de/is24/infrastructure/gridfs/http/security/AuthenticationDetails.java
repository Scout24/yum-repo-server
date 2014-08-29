package de.is24.infrastructure.gridfs.http.security;

import de.is24.infrastructure.gridfs.http.utils.HostName;


public class AuthenticationDetails {
  private final HostName remoteHost;
  private final boolean isWebRequest;

  public AuthenticationDetails(HostName remoteHost) {
    this.remoteHost = remoteHost;
    this.isWebRequest = true;
  }

  public AuthenticationDetails() {
    this.remoteHost = null;
    this.isWebRequest = false;
  }

  public HostName getRemoteHost() {
    return remoteHost;
  }

  public boolean isWebRequest() {
    return isWebRequest;
  }
}
