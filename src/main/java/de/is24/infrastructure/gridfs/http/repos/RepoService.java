package de.is24.infrastructure.gridfs.http.repos;

import de.is24.infrastructure.gridfs.http.domain.RepoEntry;
import de.is24.infrastructure.gridfs.http.domain.RepoType;
import de.is24.infrastructure.gridfs.http.exception.BadRequestException;
import de.is24.infrastructure.gridfs.http.exception.RepositoryNotFoundException;
import de.is24.infrastructure.gridfs.http.metadata.RepoEntriesRepository;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import static de.is24.infrastructure.gridfs.http.domain.RepoType.SCHEDULED;
import static de.is24.infrastructure.gridfs.http.domain.RepoType.STATIC;
import static de.is24.infrastructure.gridfs.http.domain.RepoType.VIRTUAL;
import static de.is24.infrastructure.gridfs.http.repos.RepositoryNameValidator.validateRepoName;
import static org.apache.commons.lang.ArrayUtils.contains;
import static org.apache.commons.lang.StringUtils.substringAfter;


@ManagedResource
@Service
public class RepoService {
  private static final Logger LOG = LoggerFactory.getLogger(RepoService.class);
  private static final long ONE_SEC_IN_MS = 1000;
  public static final String STATIC_PREFIX = "static/";
  public static final String VIRTUAL_PREFIX = "virtual/";
  private final RepoEntriesRepository entriesRepository;

  @Autowired
  public RepoService(RepoEntriesRepository entriesRepository) {
    this.entriesRepository = entriesRepository;
  }

  public void createOrUpdate(String reponame) {
    RepoEntry repoEntry = ensureEntry(reponame, STATIC, SCHEDULED);
    repoEntry.setLastModified(new Date());
    entriesRepository.save(repoEntry);
  }

  public void delete(String reponame) {
    RepoEntry repoEntry = entriesRepository.findFirstByName(reponame);
    if (repoEntry != null) {
      entriesRepository.delete(repoEntry);
      LOG.info("Deleted {} repository {}.", repoEntry.getType(), reponame);
    }
  }

  public void deleteVirtual(String reponame) {
    RepoEntry repoEntry = entriesRepository.findFirstByName(reponame);
    if (repoEntry != null) {
      if (VIRTUAL != repoEntry.getType()) {
        throw new BadRequestException("Repository " + reponame + " is not a VIRTUAL repository.");
      }
      entriesRepository.delete(repoEntry);
      LOG.info("Deleted {} repository {}.", repoEntry.getType(), reponame);
    }
  }

  public void updateLastMetadataGeneration(String reponame, Date date) {
    RepoEntry repoEntry = ensureEntry(reponame, STATIC, SCHEDULED);
    repoEntry.setLastMetadataGeneration(date);
    if (repoEntry.getLastModified() == null) {
      repoEntry.setLastModified(new Date(date.getTime() - ONE_SEC_IN_MS));
    }
    entriesRepository.save(repoEntry);
  }

  public boolean needsMetadataUpdate(String reponame) {
    RepoEntry repoEntry = entriesRepository.findFirstByName(reponame);
    return (repoEntry == null) ||
      (repoEntry.getLastModified() == null) ||
      (repoEntry.getLastMetadataGeneration() == null) ||
      repoEntry.getLastModified().after(repoEntry.getLastMetadataGeneration());
  }

  public boolean isRepoScheduled(String reponame) {
    RepoEntry entry = entriesRepository.findFirstByName(reponame);

    return (null != entry) && SCHEDULED.equals(entry.getType());
  }

  @ManagedOperation
  public void activateSchedulingForRepo(String reponame) {
    RepoEntry entry = ensureEntry(reponame, STATIC, SCHEDULED);
    entry.setType(SCHEDULED);
    entriesRepository.save(entry);
  }

  public RepoEntry ensureEntry(String reponame, RepoType... types) {
    RepoEntry repoEntry = entriesRepository.findFirstByName(reponame);
    if ((repoEntry != null) && (types != null) && !contains(types, repoEntry.getType())) {
      throw new IllegalArgumentException("Repository " + reponame + " found but has type: " + repoEntry.getType() +
        ". Expected: " + ArrayUtils.toString(types));
    }

    if (repoEntry == null) {
      validateRepoName(reponame);
      repoEntry = new RepoEntry();
      repoEntry.setName(reponame);
      repoEntry.setType((types != null) ? types[0] : STATIC);
    }
    return repoEntry;
  }

  @ManagedOperation
  public void setRepoType(String reponame, RepoType repoType) {
    RepoEntry repoEntry = ensureEntry(reponame, STATIC, SCHEDULED);
    if (repoType != null) {
      LOG.info("Set type of repository {} to {}", reponame, repoType);
      repoEntry.setType(repoType);
      entriesRepository.save(repoEntry);
    }
  }

  @ManagedOperation
  public void setMaxKeepRpms(String reponame, int maxKeepRpms) {
    if (maxKeepRpms < 0) {
      throw new BadRequestException("You cannot keep a negative amount of RPMs");
    }

    RepoEntry repoEntry = ensureEntry(reponame, STATIC, SCHEDULED);
    repoEntry.setMaxKeepRpms(maxKeepRpms);
    entriesRepository.save(repoEntry);
    LOG.info("Set maxKeepRpms of repository {} to {}", reponame, maxKeepRpms);
  }

  @ManagedOperation
  public void createVirtualRepo(String reponame, String destination) {
    validateRepoName(reponame);

    RepoEntry virtualEntry = ensureEntry(reponame, VIRTUAL);
    if (destination.startsWith(STATIC_PREFIX)) {
      setLinkToStatic(virtualEntry, destination);
    } else if (destination.startsWith(VIRTUAL_PREFIX)) {
      setLinkToVirtual(virtualEntry, destination);
    } else {
      setLinkToExternal(virtualEntry, destination);
    }

    entriesRepository.save(virtualEntry);
    LOG.info("Saved virtual repo '{}' linked to '{}'.", reponame, destination);
  }

  public RepoEntry getRepo(String reponame, RepoType type) {
    RepoEntry entry = entriesRepository.findFirstByNameAndType(reponame, type);
    if (entry == null) {
      throw new RepositoryNotFoundException("Could not find repository with type " + type, reponame);
    }
    return entry;
  }

  private void setLinkToExternal(RepoEntry repoEntry, String destination) {
    try {
      new URL(destination);
    } catch (MalformedURLException e) {
      throw new BadRequestException("Invalid destination repo: " + destination, e);
    }
    repoEntry.setExternal(true);
    repoEntry.setTarget(destination);
  }

  private void setLinkToVirtual(RepoEntry virtualEntry, String destination) {
    String virtualRepo = substringAfter(destination, VIRTUAL_PREFIX);
    RepoEntry repoEntry = entriesRepository.findFirstByNameAndType(virtualRepo, VIRTUAL);
    if (repoEntry == null) {
      throw new BadRequestException("Virtual repository '" + virtualRepo + "' not found.");
    }
    virtualEntry.setExternal(repoEntry.isExternal());
    virtualEntry.setTarget(repoEntry.getTarget());
  }

  private void setLinkToStatic(RepoEntry virtualEntry, String destination) {
    virtualEntry.setExternal(false);
    virtualEntry.setTarget(substringAfter(destination, STATIC_PREFIX));

    RepoEntry repoEntry = entriesRepository.findFirstByName(virtualEntry.getTarget());
    if ((repoEntry == null) || ((repoEntry.getType() != STATIC) && (repoEntry.getType() != SCHEDULED))) {
      throw new BadRequestException("Static repository '" + virtualEntry.getTarget() + "' not found.");
    }
  }
}
