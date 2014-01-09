package de.is24.infrastructure.gridfs.http.security;

import de.is24.infrastructure.gridfs.http.utils.HostName;
import de.is24.infrastructure.gridfs.http.utils.HostnameResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.stereotype.Component;
import javax.servlet.http.HttpServletRequest;
import java.util.Set;
import static org.springframework.util.StringUtils.commaDelimitedListToSet;
import static org.springframework.util.StringUtils.trimAllWhitespace;


@Component
public class WhiteListAuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter {
  private static final Logger LOGGER = LoggerFactory.getLogger(WhiteListAuthenticationFilter.class);

  public static final String REMOTE_HOST_KEY = "security.remote.host";
  private final Set<String> whiteListedHosts;
  private final HostnameResolver hostnameResolver;

  @Autowired
  public WhiteListAuthenticationFilter(@Value("${security.whitelist.hosts:}") String whiteListedHosts,
                                       AuthenticationManager authenticationManager,
                                       HostnameResolver hostnameResolver) {
    this.hostnameResolver = hostnameResolver;
    this.whiteListedHosts = commaDelimitedListToSet(trimAllWhitespace(whiteListedHosts));
    setAuthenticationManager(authenticationManager);
  }

  @Override
  protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
    HostName hostName = hostnameResolver.remoteHost(request);
    request.setAttribute(REMOTE_HOST_KEY, hostName);

    if (whiteListedHosts.contains(hostName.getName())) {
      return hostName.getName();
    }

    return null;
  }

  @Override
  protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
    return getPreAuthenticatedPrincipal(request);
  }
}
