package de.is24.infrastructure.gridfs.http.security;

import de.is24.infrastructure.gridfs.http.utils.HostName;
import de.is24.infrastructure.gridfs.http.utils.HostnameResolver;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.stereotype.Component;
import javax.servlet.http.HttpServletRequest;
import java.util.Set;

import static org.apache.commons.lang.StringUtils.join;
import static org.springframework.util.StringUtils.commaDelimitedListToSet;
import static org.springframework.util.StringUtils.trimAllWhitespace;


@Component
@ManagedResource
public class WhiteListAuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter {
  private static final Logger LOGGER = LoggerFactory.getLogger(WhiteListAuthenticationFilter.class);
  private static final String WHITE_LISTED_HOSTS_MODIFCATION_ENABLED_KEY = "security.whitelist.modification.enabled";

  private Set<String> whiteListedHosts;
  private final HostnameResolver hostnameResolver;
  private final boolean whiteListModificationEnabled;

  @Autowired
  public WhiteListAuthenticationFilter(@Value("${security.whitelist.hosts:}") String whiteListedHosts,
                                       @Value("${" + WHITE_LISTED_HOSTS_MODIFCATION_ENABLED_KEY + ":false}") boolean whiteListModificationEnabled,
                                       AuthenticationManager authenticationManager,
                                       HostnameResolver hostnameResolver) {
    this.hostnameResolver = hostnameResolver;
    this.whiteListedHosts = parseWhiteListedHosts(whiteListedHosts);
    this.whiteListModificationEnabled = whiteListModificationEnabled;
    setAuthenticationManager(authenticationManager);
  }

  @Override
  protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
    HostName hostName = hostnameResolver.remoteHost(request);

    if (whiteListedHosts.contains(hostName.getName())) {
      return hostName.getName();
    }

    return null;
  }

  @Override
  protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
    return getPreAuthenticatedPrincipal(request);
  }

  @ManagedAttribute
  public String getWhiteListedHosts() {
    return join(whiteListedHosts, ",");
  }

  @ManagedAttribute
  public void setWhiteListedHosts(String whiteListedHosts) {
    if (!whiteListModificationEnabled) {
      throw new IllegalStateException("Modifying white listed hosts is not permitted. Please enable it via " +
          WHITE_LISTED_HOSTS_MODIFCATION_ENABLED_KEY +
          " in your configuration.");
    }

    this.whiteListedHosts = parseWhiteListedHosts(whiteListedHosts);
  }

  protected Set<String> parseWhiteListedHosts(String whiteListedHosts) {
    return commaDelimitedListToSet(trimAllWhitespace(whiteListedHosts));
  }
}
