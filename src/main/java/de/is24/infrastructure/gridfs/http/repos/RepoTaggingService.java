package de.is24.infrastructure.gridfs.http.repos;

import de.is24.infrastructure.gridfs.http.domain.RepoEntry;
import de.is24.infrastructure.gridfs.http.exception.RepositoryNotFoundException;
import de.is24.infrastructure.gridfs.http.metadata.RepoEntriesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.Set;


/**
 * @author twalter
 * @since 4/8/13
 */
@Service
public class RepoTaggingService {
  private static final Logger LOG = LoggerFactory.getLogger(RepoTaggingService.class);

  private final RepoEntriesRepository entriesRepository;

  @Autowired
  public RepoTaggingService(RepoEntriesRepository entriesRepository) {
    this.entriesRepository = entriesRepository;
  }

  public void addTag(String repoName, String repoTag) {
    final RepoEntry repoEntry = ensureEntry(repoName);
    repoEntry.getTags().add(repoTag);

    entriesRepository.save(repoEntry);
    LOG.info("add tag {} for repo {} ", repoTag, repoName);
  }

  public Set<String> getTags(String reponame) {
    final RepoEntry repoEntry = ensureEntry(reponame);
    return repoEntry.getTags();
  }

  public void deleteAllTags(String repoName) {
    final RepoEntry repoEntry = ensureEntry(repoName);

    repoEntry.setTags(Collections.<String>emptySet());
    entriesRepository.save(repoEntry);
    LOG.info("deleted all tags for repo {} ", repoName);
  }

  public void deleteTag(String repoName, String repoTag) {
    final RepoEntry repoEntry = ensureEntry(repoName);

    repoEntry.getTags().remove(repoTag);
    entriesRepository.save(repoEntry);
    LOG.info("deleted tag {} for repo {} ", repoTag, repoName);
  }

  private RepoEntry ensureEntry(String repoName) {
    final RepoEntry repoEntry = entriesRepository.findFirstByName(repoName);
    if (repoEntry == null) {
      throw new RepositoryNotFoundException("repository not found!", repoName);
    }
    return repoEntry;
  }
}
