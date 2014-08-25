package de.is24.infrastructure.gridfs.http.security;

import de.is24.infrastructure.gridfs.http.utils.HostName;
import de.is24.infrastructure.gridfs.http.utils.HostnameResolver;
import de.is24.infrastructure.gridfs.http.utils.WildcardToRegexConverter;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import javax.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.Set;
import java.util.regex.Pattern;

import static java.nio.charset.Charset.forName;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang.StringUtils.substringBefore;
import static org.springframework.util.StringUtils.commaDelimitedListToSet;
import static org.springframework.util.StringUtils.trimAllWhitespace;


@ManagedResource
public class WhiteListAuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter {
  public static final String WHITE_LISTED_HOSTS_MODIFCATION_ENABLED_KEY = "security.whitelist.modification.enabled";
  public static final String BASIC_AUTH_PREFIX = "Basic ";

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
    return isWhiteListedHost(hostName) ? getUsernameFrom(request, hostName.getName()) : null;
  }

  @Override
  protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
    HostName hostName = hostnameResolver.remoteHost(request);
    return isWhiteListedHost(hostName) ? hostName.getName() : null;
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

  protected String getUsernameFrom(HttpServletRequest request, String defaultUsername) {
    String authorizationHeader = request.getHeader("Authorization");
    String usernameHeader = request.getHeader("Username");
    if (authorizationHeader != null && authorizationHeader.startsWith(BASIC_AUTH_PREFIX)) {
      return getBasicAuthUser(authorizationHeader);
    } else if (usernameHeader != null) {
      return usernameHeader;
    }

    return defaultUsername;
  }

  private String getBasicAuthUser(String authorizationHeader) {
    String base64Credentials = authorizationHeader.substring(BASIC_AUTH_PREFIX.length());
    String credentials = new String(Base64.getDecoder().decode(base64Credentials), forName("UTF-8"));
    return substringBefore(credentials, ":");
  }

  protected boolean isWhiteListedHost(HostName hostName) {
    for (Pattern whiteListedHostPattern : whiteListedHostPatterns) {
      if (whiteListedHostPattern.matcher(hostName.getName()).matches()) {
        return true;
      }
    }
    return false;
  }
}
