package de.is24.infrastructure.gridfs.http.log4j;

import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import static de.is24.infrastructure.gridfs.http.security.WhiteListAuthenticationFilter.REMOTE_HOST_KEY;
import static org.apache.commons.lang.StringUtils.isBlank;


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

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse res,
                       FilterChain chain) throws IOException, ServletException {
    try {
      String remoteHost = remoteHost(req);
      MDC.put(REMOTE_HOST, isBlank(remoteHost) ? NOT_AVAILABLE : remoteHost);
      MDC.put(PRINCIPAL, (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
      chain.doFilter(req, res);
    } finally {
      MDC.remove(REMOTE_HOST);
      MDC.remove(PRINCIPAL);
    }
  }

  private String remoteHost(ServletRequest req) {
    Object remoteHostAttr = req.getAttribute(REMOTE_HOST_KEY);
    if (remoteHostAttr != null) {
      return remoteHostAttr.toString();
    }

    return req.getRemoteAddr();
  }

  @Override
  public void destroy() {
  }

}
