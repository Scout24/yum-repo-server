package de.is24.infrastructure.gridfs.http.security;

import de.is24.infrastructure.gridfs.http.storage.FileDescriptor;
import de.is24.infrastructure.gridfs.http.utils.HostName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.util.Collections.synchronizedSet;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.springframework.util.StringUtils.commaDelimitedListToSet;
import static org.springframework.util.StringUtils.trimAllWhitespace;


@Component
@ManagedResource
public class ProtectedRepoAccessEvaluator {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProtectedRepoAccessEvaluator.class);

  private final Set<String> protectedRepos;
  private List<IpRange> whiteListedIpRanges = new ArrayList<>();

  @Autowired
  public ProtectedRepoAccessEvaluator(
    @Value("${security.protectedRepos:}") String protectedRepos,
    @Value("${security.protectedRepoWhiteListedIpRanges:}") String protectedRepoWhiteListedIpRanges) {
    this.protectedRepos = synchronizedSet(ipRanges(protectedRepos));
    if (isNotBlank(protectedRepoWhiteListedIpRanges)) {
      whiteListedIpRanges.addAll(
        ipRanges(protectedRepoWhiteListedIpRanges).stream().map(IpRange::new).collect(toList()));
    }
  }

  private Set<String> ipRanges(String protectedRepoWhiteListedIpRanges) {
    return commaDelimitedListToSet(trimAllWhitespace(protectedRepoWhiteListedIpRanges));
  }

  public boolean isAllowedPropagationRepo(String repo) {
    return !protectedRepos.contains(repo);
  }

  public boolean isAllowed(FileDescriptor fileDescriptor, Authentication authentication) {
    if (isAuthenticatedUser(authentication)) {
      LOGGER.debug("...allowed because authenticated user.");
      return true;
    }

    if (isBackendCall(authentication)) {
      LOGGER.debug("...allowed because backend call.");
      return true;
    }

    if (isNotCallOnProtectedRepo(fileDescriptor)) {
      LOGGER.debug("...allowed because unprotected repo.");
      return true;
    }

    HostName remoteHost = ((AuthenticationDetails) (authentication.getDetails())).getRemoteHost();
    if (isAllowedWebCallOnProtectedRepo(fileDescriptor, remoteHost)) {
      LOGGER.debug("...allowed because authorized call to protected repo.");
      return true;
    }

    return false;
  }

  public boolean isBackendCall(Authentication authentication) {
    if (hasAuthenticationDetails(authentication)) {
      return !((AuthenticationDetails) (authentication.getDetails())).isWebRequest();
    }

    logUnknownAuthentication(authentication);
    return true;
  }

  private boolean isAuthenticatedUser(Authentication authentication) {
    return authentication != null && authentication instanceof UsernamePasswordAuthenticationToken;
  }

  private static boolean hasAuthenticationDetails(Authentication authentication) {
    return (authentication != null) && (authentication.getDetails() instanceof AuthenticationDetails);
  }

  private boolean isNotCallOnProtectedRepo(FileDescriptor fileDescriptor) {
    return fileDescriptor.getArch().equals("repodata") || !protectedRepos.contains(fileDescriptor.getRepo());
  }

  public boolean isAllowedWebCallOnProtectedRepo(FileDescriptor fileDescriptor, HostName remoteHostName) {
    LOGGER.info("check access permission for {} to {}", remoteHostName, fileDescriptor.getPath());
    if (remoteHostName.isIp()) {
      LOGGER.debug("..is IP...");
      if (!isAllowedIp(remoteHostName.getName())) {
        LOGGER.info("... ip not in whitelist: deny");
        return false;
      }
    } else if (!fileDescriptor.getFilename().contains(remoteHostName.getShortName())) {
      LOGGER.info("... not ip, not matching: deny");
      return false;
    }
    return true;
  }

  public void logUnknownAuthentication(Authentication authentication) {
    if ((authentication == null) || (authentication.getDetails() == null)) {
      LOGGER.warn("encountered null authentication or null authentication details");
    } else {
      LOGGER.warn("encountered unexpected authentication details type {}",
        authentication.getDetails().getClass().getName());
    }
  }

  private boolean isAllowedIp(String ip) {
    for (IpRange ipRange : whiteListedIpRanges) {
      if (ipRange.isIn(ip)) {
        return true;
      }
    }

    return false;
  }

  @ManagedOperation
  public void addProtectedRepo(String repoName) {
    protectedRepos.add(repoName);
  }

  @ManagedOperation
  public Set<String> getProtectedRepos() {
    return Collections.unmodifiableSet(protectedRepos);
  }
}
