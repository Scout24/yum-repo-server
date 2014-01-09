package de.is24.infrastructure.gridfs.http.security;

import de.is24.infrastructure.gridfs.http.utils.HostName;
import de.is24.infrastructure.gridfs.http.utils.HostnameResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;


@Component
public class HostNameFilter implements Filter {
  public static final String REMOTE_HOST_NAME = "hostnameFilter.hostname";
  private HostnameResolver hostnameResolver;

  @Autowired
  public HostNameFilter(HostnameResolver hostnameResolver) {
    this.hostnameResolver = hostnameResolver;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                                   ServletException {
    if (request instanceof HttpServletRequest) {
      HostName hostName = hostnameResolver.remoteHost((HttpServletRequest) request);
      request.setAttribute(REMOTE_HOST_NAME, hostName);
    }
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
  }
}
