package de.is24.infrastructure.gridfs.http.utils;

import org.apache.log4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import static de.is24.infrastructure.gridfs.http.log4j.MDCFilter.PRINCIPAL;
import static de.is24.infrastructure.gridfs.http.log4j.MDCFilter.REMOTE_HOST;
import static de.is24.infrastructure.gridfs.http.log4j.MDCFilter.SERVER_NAME;


public class MDCHelper implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(MDCHelper.class);

  public MDCHelper(Class callerClass) {
    MDC.put(REMOTE_HOST, callerClass.getName());
    MDC.put(SERVER_NAME, "localhost");

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null) {
      MDC.put(PRINCIPAL, (String) authentication.getPrincipal());
    } else {
      MDC.put(PRINCIPAL, "no authentication set");
    }
  }

  @Override
  public void close() {
    MDC.remove(SERVER_NAME);
    MDC.remove(PRINCIPAL);
    MDC.remove(REMOTE_HOST);
  }
}
