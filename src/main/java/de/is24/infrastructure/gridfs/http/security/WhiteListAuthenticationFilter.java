package de.is24.infrastructure.gridfs.http.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Set;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.apache.commons.lang.StringUtils.trim;
import static org.springframework.util.StringUtils.commaDelimitedListToSet;
import static org.springframework.util.StringUtils.trimAllWhitespace;

@Component
public class WhiteListAuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter {

  public static final String REMOTE_HOST_KEY = "security.remote.host";
  private static final String X_FORWARDED_FOR = "X-Forwarded-For";
  public static final String ADDRESS_SEPERATOR = ",";
  private final Set<String> whiteListedHosts;
  private final Set<String> loadBalancerIPs;

  @Autowired
  public WhiteListAuthenticationFilter(
      @Value("${security.whitelist.hosts:}") String whiteListedHosts,
      @Value("${security.loadbalancer.ips:}") String loadBalancerIPs,
      AuthenticationManager authenticationManager) {
    this.whiteListedHosts = commaDelimitedListToSet(trimAllWhitespace(whiteListedHosts));
    this.loadBalancerIPs = commaDelimitedListToSet(trimAllWhitespace(loadBalancerIPs));
    setAuthenticationManager(authenticationManager);
  }

  @Override
  protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
    String remoteHost = remoteHost(request);
    request.setAttribute(REMOTE_HOST_KEY, remoteHost);

    if (whiteListedHosts.contains(remoteHost)) {
      return remoteHost;
    }

    return null;
  }

  private String remoteHost(HttpServletRequest request) {
    if (loadBalancerIPs.contains(request.getRemoteAddr()) && isNotBlank(request.getHeader(X_FORWARDED_FOR))) {
      return hostname(lastAddress(request.getHeader(X_FORWARDED_FOR)));
    }

    return hostname(request.getRemoteHost());
  }

  private String lastAddress(String header) {
    if (isNotBlank(header) && header.contains(ADDRESS_SEPERATOR)) {
      return trim(substringAfterLast(header, ADDRESS_SEPERATOR));
    }

    return header;
  }

  private String hostname(String hostnameOrIP) {
    if (isIPAddress(hostnameOrIP)) {
      try {
        return Inet4Address.getByName(hostnameOrIP).getHostName();
      } catch (UnknownHostException e) {
        return hostnameOrIP;
      }
    }

    return hostnameOrIP;
  }

  private boolean isIPAddress(String hostnameOrIP) {
    return hostnameOrIP != null && hostnameOrIP.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
  }

  @Override
  protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
    return getPreAuthenticatedPrincipal(request);
  }
}
