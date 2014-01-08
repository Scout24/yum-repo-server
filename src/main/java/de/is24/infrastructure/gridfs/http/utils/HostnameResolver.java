package de.is24.infrastructure.gridfs.http.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
public class HostnameResolver {
  private static final Logger LOGGER = LoggerFactory.getLogger(HostnameResolver.class);

  protected static final String X_FORWARDED_FOR = "X-Forwarded-For";
  public static final String ADDRESS_SEPERATOR = ",";
  private final Set<String> loadBalancerIPs;

  @Autowired
  public HostnameResolver(@Value("${loadbalancer.ips:}") String loadBalancerIPs) {
    LOGGER.info("Initializing HostnameResolver with loadbalancer IPs {}", loadBalancerIPs);
    this.loadBalancerIPs = commaDelimitedListToSet(trimAllWhitespace(loadBalancerIPs));
  }

  public String remoteHost(HttpServletRequest request) {
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
    String result = hostnameOrIP;
    if (isIPAddress(hostnameOrIP)) {
      try {
        result = Inet4Address.getByName(hostnameOrIP).getHostName();
      } catch (UnknownHostException e) {
        LOGGER.info("could not resolve hostname for {}", hostnameOrIP);
      }
    }
    LOGGER.debug("resolved hostname for {} is {}", hostnameOrIP, result);
    return result;
  }

  private boolean isIPAddress(String hostnameOrIP) {
    return (hostnameOrIP != null) && hostnameOrIP.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
  }

}
