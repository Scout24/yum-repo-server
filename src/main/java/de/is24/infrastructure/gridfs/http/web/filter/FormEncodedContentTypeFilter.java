package de.is24.infrastructure.gridfs.http.web.filter;

import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import static org.apache.commons.lang.StringUtils.isBlank;


@Component
public class FormEncodedContentTypeFilter extends OncePerRequestFilter {
  private final FormHttpMessageConverter messageConverter;

  public FormEncodedContentTypeFilter() {
    this.messageConverter = new FormHttpMessageConverter();
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                           throws ServletException, IOException {
    if (isPost(request) && isContentTypeNotSet(request)) {
      request = new FormEncodedHttpServletRequestWrapper(request, messageConverter);
    }
    chain.doFilter(request, response);
  }

  private boolean isContentTypeNotSet(HttpServletRequest request) {
    return isBlank(request.getHeader("Content-Type"));
  }

  private boolean isPost(HttpServletRequest request) {
    return "POST".equals(request.getMethod());
  }
}
