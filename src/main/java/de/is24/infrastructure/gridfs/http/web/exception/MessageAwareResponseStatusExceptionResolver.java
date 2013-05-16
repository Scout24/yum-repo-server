package de.is24.infrastructure.gridfs.http.web.exception;

import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MessageAwareResponseStatusExceptionResolver extends ResponseStatusExceptionResolver {

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Override
  protected ModelAndView resolveResponseStatus(ResponseStatus responseStatus, HttpServletRequest request,
                                               HttpServletResponse response, Object handler, Exception ex) throws Exception {
    int statusCode = responseStatus.value().value();
    String reason = responseStatus.reason();
    if (!StringUtils.hasLength(reason)) {
      response.sendError(statusCode, ex.getMessage());
    } else {
      response.sendError(statusCode, reason);
    }
    return new ModelAndView();

  }
}
