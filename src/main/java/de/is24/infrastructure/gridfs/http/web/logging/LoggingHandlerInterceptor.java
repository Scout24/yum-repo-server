package de.is24.infrastructure.gridfs.http.web.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class LoggingHandlerInterceptor extends HandlerInterceptorAdapter {
  public static final Logger LOG = LoggerFactory.getLogger(LoggingHandlerInterceptor.class);

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
                       throws Exception {
    if (ex != null) {
      LOG.error("There was thrown an exception in handler: {} on {} {}", handler, request.getRequestURI(),
        request.getMethod(), ex);
    }
  }
}
