package de.is24.infrastructure.gridfs.http.security;

import de.is24.infrastructure.gridfs.http.gridfs.GridFsFileDescriptor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import java.util.HashSet;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;


@Component
public class HostNamePatternFilter {
  private HashSet<String> protectedRepos = new HashSet<String>();


  public boolean isAllowed(GridFsFileDescriptor gridFsFileDescriptor) {
    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

    String remoteHostName = (String) requestAttributes.getAttribute(WhiteListAuthenticationFilter.REMOTE_HOST_KEY,
      SCOPE_REQUEST);

    if (protectedRepos.contains(gridFsFileDescriptor.getRepo())) {
      if (gridFsFileDescriptor.getFilename().contains(remoteHostName) ||
          gridFsFileDescriptor.getArch().equals("repodata")) {
        return true;
      } else {
        return false;
      }
    }

    return true;
  }

  public void addProtectedRepo(String repoName) {
    protectedRepos.add(repoName);
  }
}
