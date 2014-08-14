package de.is24.infrastructure.gridfs.http.security;

import de.is24.infrastructure.gridfs.http.utils.HostName;
import de.is24.infrastructure.gridfs.http.utils.HostnameResolver;
import de.is24.infrastructure.gridfs.http.utils.WildcardToRegexConverter;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toSet;
import static org.springframework.util.StringUtils.commaDelimitedListToSet;
import static org.springframework.util.StringUtils.trimAllWhitespace;


@ManagedResource
public class WhiteListAuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter {
  public static final String WHITE_LISTED_HOSTS_MODIFCATION_ENABLED_KEY = "security.whitelist.modification.enabled";

  private String whiteListedHosts;
  private Set<Pattern> whiteListedHostPatterns;
  private final HostnameResolver hostnameResolver;
  private final boolean whiteListModificationEnabled;
  private final WildcardToRegexConverter wildcardToRegexConverter = new WildcardToRegexConverter();

  public WhiteListAuthenticationFilter(String whiteListedHosts,
                                       boolean whiteListModificationEnabled,
                                       AuthenticationManager authenticationManager,
                                       HostnameResolver hostnameResolver) {
    this.hostnameResolver = hostnameResolver;
    this.whiteListModificationEnabled = whiteListModificationEnabled;
    setWhiteListedHostsInternal(whiteListedHosts);
    setAuthenticationManager(authenticationManager);
  }

  @Override
  protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
    HostName hostName = hostnameResolver.remoteHost(request);
    for (Pattern whiteListedHostPattern : whiteListedHostPatterns) {
      if (whiteListedHostPattern.matcher(hostName.getName()).matches()) {
        return hostName.getName();
      }
    }

    return null;
  }

  @Override
  protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
    return getPreAuthenticatedPrincipal(request);
  }

  @ManagedAttribute
  public String getWhiteListedHosts() {
    return whiteListedHosts;
  }

  @ManagedAttribute
  public void setWhiteListedHosts(String whiteListedHosts) {
    if (!whiteListModificationEnabled) {
      throw new IllegalStateException("Modifying white listed hosts is not permitted. Please enable it via " +
          WHITE_LISTED_HOSTS_MODIFCATION_ENABLED_KEY +
          " in your configuration.");
    }

    setWhiteListedHostsInternal(whiteListedHosts);
  }

  protected void setWhiteListedHostsInternal(String whiteListedHosts) {
    this.whiteListedHosts = whiteListedHosts;
    Set<String> whiteListedHostsStrings = commaDelimitedListToSet(trimAllWhitespace(whiteListedHosts));
    this.whiteListedHostPatterns = whiteListedHostsStrings.stream().map(wildcardToRegexConverter::convert).collect(toSet());
  }
}
