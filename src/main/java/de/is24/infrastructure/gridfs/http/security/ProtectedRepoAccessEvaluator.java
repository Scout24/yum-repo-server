package de.is24.infrastructure.gridfs.http.security;

import de.is24.infrastructure.gridfs.http.storage.FileDescriptor;
import de.is24.infrastructure.gridfs.http.utils.HostName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
    this.protectedRepos = Collections.synchronizedSet(commaDelimitedListToSet(trimAllWhitespace(protectedRepos)));
    if (isNotBlank(protectedRepoWhiteListedIpRanges)) {
      for (String ipRange : commaDelimitedListToSet(trimAllWhitespace(protectedRepoWhiteListedIpRanges))) {
        whiteListedIpRanges.add(new IpRange(ipRange));
      }
    }
  }

  public boolean isAllowedPropagationRepo(String repo) {
    return !protectedRepos.contains(repo);
  }

  public boolean isAllowed(FileDescriptor fileDescriptor, Authentication authentication) {
    boolean isWebCall = false;
    HostName remoteHostName = null;
    if (authentication != null) {
      isWebCall = true;
      remoteHostName = ((AuthenticationDetails) authentication.getDetails()).getRemoteHost();
    }

    if (isWebCall && !fileDescriptor.getArch().equals("repodata") &&
        protectedRepos.contains(fileDescriptor.getRepo())) {
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
    }
    LOGGER.debug("...allowed.");

    return true;
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
