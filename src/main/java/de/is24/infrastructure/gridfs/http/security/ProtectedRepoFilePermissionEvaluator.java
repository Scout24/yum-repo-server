package de.is24.infrastructure.gridfs.http.security;

import de.is24.infrastructure.gridfs.http.gridfs.GridFsFileDescriptor;
import de.is24.util.monitoring.InApplicationMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class ProtectedRepoFilePermissionEvaluator implements PermissionEvaluator {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProtectedRepoFilePermissionEvaluator.class);
  private static final String APPMON_BASE_KEY = "GridFsService.";
  private static final String APPMON_ACCESS_PREVENTION = APPMON_BASE_KEY + "preventAccess";

  private final HostNamePatternFilter accessFilter;

  @Autowired
  public ProtectedRepoFilePermissionEvaluator(HostNamePatternFilter accessFilter) {
    this.accessFilter = accessFilter;
  }

  @Override
  public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
    if (targetDomainObject != null && isGridFsFileDescriptor(targetDomainObject)) {
      return hasPermission((GridFsFileDescriptor) targetDomainObject);
    }

    return true;
  }

  private boolean hasPermission(GridFsFileDescriptor descriptor) {
    if (!accessFilter.isAllowed(descriptor)) {
      InApplicationMonitor.getInstance().incrementCounter(APPMON_ACCESS_PREVENTION);
      LOGGER.warn("preventing access to {}", descriptor.getPath());
      return false;
    }

    return true;
  }

  private boolean isGridFsFileDescriptor(Object targetDomainObject) {
    return GridFsFileDescriptor.class.isAssignableFrom(targetDomainObject.getClass());
  }

  @Override
  public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
    return false;
  }
}
