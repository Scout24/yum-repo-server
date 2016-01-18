package de.is24.infrastructure.gridfs.http.web;

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.MOVED_PERMANENTLY;
import static org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromRequest;

public class TrailingSlashRedirectHandlerInterceptor extends HandlerInterceptorAdapter {

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    ServletUriComponentsBuilder builder = fromRequest(request);
    String path = builder.build().getPath();
    if (isGetRequest(request) && isRepoRequestOrArch(path) && isNotWildcardExtensionMatching(request) && !isFileRequest(path)) {
      builder.replacePath(path + '/');
      String location = builder.build().toString();
      response.setStatus(MOVED_PERMANENTLY.value());
      response.setHeader("Location", location);
      return false;
    }

    return true;
  }

  private boolean isNotWildcardExtensionMatching(HttpServletRequest request) {
    String bestMatchingPattern = (String) request.getAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE);
    return bestMatchingPattern == null || !bestMatchingPattern.endsWith("*");
  }

  private boolean isRepoRequestOrArch(String path) {
    return path.matches("^/repo(/virtual)?(/([^/]+)(/[^/]+)?)?$");
  }

  private boolean isFileRequest(String path) {
    return path.matches("^.*\\.(html|json)?$");
  }

  private boolean isGetRequest(HttpServletRequest request) {
    return GET.name().equals(request.getMethod());
  }
}
