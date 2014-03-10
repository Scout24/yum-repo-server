package de.is24.infrastructure.gridfs.http.log4j;

import de.is24.infrastructure.gridfs.http.security.AuthenticationDetails;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.*;
import java.io.IOException;

import static org.apache.commons.lang.StringUtils.defaultIfBlank;


/**
 * The MDCFilter makes custom values available in log4j. See Log4j's Mapped
 * Diagnostic Context (MDC).
 *
 * Usage: %X{remoteIP} in log4j.properties
 *
 * @author Tgass
 *
 */
public class MDCFilter implements Filter {
  private static final String NOT_AVAILABLE = "not_available";
  public static final String REMOTE_HOST = "remoteHost";
  public static final String PRINCIPAL = "principal";
  public static final String SERVER_NAME = "serverName";

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse res,
                       FilterChain chain) throws IOException, ServletException {
    try {
      MDC.put(REMOTE_HOST, defaultIfBlank(remoteHost(req), NOT_AVAILABLE));
      MDC.put(SERVER_NAME, defaultIfBlank(req.getServerName(), NOT_AVAILABLE));
      MDC.put(PRINCIPAL, (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
      chain.doFilter(req, res);
    } finally {
      MDC.remove(REMOTE_HOST);
      MDC.remove(SERVER_NAME);
      MDC.remove(PRINCIPAL);
    }
  }

  private String remoteHost(ServletRequest req) {
    AuthenticationDetails details = (AuthenticationDetails) SecurityContextHolder.getContext().getAuthentication().getDetails();
    if (details.getRemoteHost() != null) {
      return details.getRemoteHost().toString();
    }

    return req.getRemoteAddr();
  }

  @Override
  public void destroy() {
  }

}
