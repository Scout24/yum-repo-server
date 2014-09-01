package de.is24.infrastructure.gridfs.http.utils;

import org.apache.log4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static de.is24.infrastructure.gridfs.http.log4j.MDCFilter.PRINCIPAL;
import static de.is24.infrastructure.gridfs.http.log4j.MDCFilter.REMOTE_HOST;
import static de.is24.infrastructure.gridfs.http.log4j.MDCFilter.SERVER_NAME;


public class MDCHelper {

  private final Class callerClass;

  public MDCHelper(Class callerClass) {
    this.callerClass = callerClass;
  }

  public void run(Runnable action) {
    prepareMDC();
    try {
      action.run();
    } finally {
      disposeMDC();
    }
  }

  public void prepareMDC() {
    MDC.put(REMOTE_HOST, callerClass.getName());
    MDC.put(SERVER_NAME, "localhost");

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null) {
      MDC.put(PRINCIPAL, authentication.getPrincipal());
    } else {
      MDC.put(PRINCIPAL, "no authentication set");
    }
  }

  public void disposeMDC() {
    MDC.remove(SERVER_NAME);
    MDC.remove(PRINCIPAL);
    MDC.remove(REMOTE_HOST);
  }
}
