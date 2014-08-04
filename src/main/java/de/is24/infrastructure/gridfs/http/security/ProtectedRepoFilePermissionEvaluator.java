package de.is24.infrastructure.gridfs.http.security;

import de.is24.infrastructure.gridfs.http.storage.FileDescriptor;
import de.is24.util.monitoring.InApplicationMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.Serializable;

import static de.is24.infrastructure.gridfs.http.security.Permission.PROPAGATE_FILE;
import static de.is24.infrastructure.gridfs.http.security.Permission.PROPAGATE_REPO;
import static de.is24.infrastructure.gridfs.http.security.Permission.READ_FILE;

@Component
public class ProtectedRepoFilePermissionEvaluator implements PermissionEvaluator {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProtectedRepoFilePermissionEvaluator.class);
  private static final String APPMON_BASE_KEY = "GridFsService.";
  private static final String APPMON_ACCESS_PREVENTION = APPMON_BASE_KEY + "preventAccess";

  private final ProtectedRepoAccessEvaluator accessEvaluator;

  @Autowired
  public ProtectedRepoFilePermissionEvaluator(ProtectedRepoAccessEvaluator accessEvaluator) {
    this.accessEvaluator = accessEvaluator;
  }

  @Override
  public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
    if (READ_FILE.equals(permission)) {
      Assert.notNull(targetDomainObject);
      Assert.isInstanceOf(FileDescriptor.class, targetDomainObject, "GridFsFileDescriptor expected for read file permission");
      return hasReadFilePermission((FileDescriptor) targetDomainObject, authentication);
    }

    if (PROPAGATE_FILE.equals(permission)) {
      Assert.notNull(targetDomainObject);
      Assert.isInstanceOf(String.class, targetDomainObject, "String exepected for propagate file permission");
      return hasReadFilePermission(new FileDescriptor(targetDomainObject.toString()), authentication);
    }

    if (PROPAGATE_REPO.equals(permission)) {
      Assert.notNull(targetDomainObject);
      Assert.isInstanceOf(String.class, targetDomainObject, "String exepected for propagate repository permission");
      return hasPropagateRepoPermission(targetDomainObject.toString());
    }

    throw new IllegalArgumentException("Unknown permission: " + permission.toString());
  }

  private boolean hasReadFilePermission(FileDescriptor descriptor, Authentication authentication) {
    if (!accessEvaluator.isAllowed(descriptor, authentication)) {
      InApplicationMonitor.getInstance().incrementCounter(APPMON_ACCESS_PREVENTION);
      LOGGER.warn("preventing access to {}", descriptor.getPath());
      return false;
    }

    return true;
  }

  private boolean hasPropagateRepoPermission(String repo) {
    if (!accessEvaluator.isAllowedPropagationRepo(repo)) {
      InApplicationMonitor.getInstance().incrementCounter(APPMON_ACCESS_PREVENTION);
      LOGGER.warn("preventing access to {}", repo);
      return false;
    }

    return true;
  }


  @Override
  public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
    return false;
  }
}
