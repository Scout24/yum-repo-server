package de.is24.infrastructure.gridfs.http.metadata.scheduling;

import de.is24.infrastructure.gridfs.http.domain.RepoEntry;
import de.is24.infrastructure.gridfs.http.metadata.MetadataService;
import de.is24.infrastructure.gridfs.http.metadata.RepoEntriesRepository;
import de.is24.infrastructure.gridfs.http.mongo.MongoPrimaryDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static de.is24.infrastructure.gridfs.http.domain.RepoType.SCHEDULED;
import static java.util.stream.Collectors.toSet;


@ManagedResource
@Service
public class RepoMetadataScheduler {
  private static final Logger LOG = LoggerFactory.getLogger(RepoMetadataScheduler.class);

  private final RepoEntriesRepository repo;
  private final MetadataService metadataService;
  private final MongoPrimaryDetector primaryDetector;
  private final TaskScheduler taskScheduler;
  private final int delayInSec;
  private Map<String, RepoMetadataGeneratorJob> repoJobs;

  @Autowired
  public RepoMetadataScheduler(RepoEntriesRepository repo,
                               MetadataService metadataService,
                               MongoPrimaryDetector primaryDetector,
                               TaskScheduler taskScheduler,
                               @Value("${scheduler.delay:10}") int delayInSec) {
    this.repo = repo;
    this.metadataService = metadataService;
    this.primaryDetector = primaryDetector;
    this.taskScheduler = taskScheduler;
    this.delayInSec = delayInSec;
    this.repoJobs = new HashMap<>();
  }

  @Scheduled(cron = "${scheduler.update.cron:*/30 * * * * *}")
  public void update() {
    LOG.debug("Checking for updates in scheduled repository definitions.");
    try {
      Set<String> repoNamesToSchedule = repo.findByType(SCHEDULED).stream().map(RepoEntry::getName).collect(toSet());
      repoNamesToSchedule.forEach(this::createRepoJob);
      removeJobsNotFoundInDb(repoNamesToSchedule);
    } catch (Exception e) {
      LOG.error(e.getMessage());
    }
  }

  private void removeJobsNotFoundInDb(Set<String> repoNamesToSchedule) {
    for (String repoName : new HashSet<>(repoJobs.keySet())) {
      if (!repoNamesToSchedule.contains(repoName)) {
        removeRepoJob(repoName);
      }
    }
  }

  @ManagedOperation
  public void removeRepoJob(String repoName) {
    repoJobs.get(repoName).deactivate();
    repoJobs.remove(repoName);
    LOG.info("Removed scheduling job for repository: {}", repoName);
  }

  @ManagedOperation
  public void createRepoJob(String name) {
    if (!repoJobs.containsKey(name)) {
      repoJobs.put(name,
        new RepoMetadataGeneratorJob(name, metadataService, primaryDetector, taskScheduler, delayInSec));
      LOG.info("Added scheduling job for repository: {}", name);
    }
  }

  @ManagedOperation
  public Map<String, RepoMetadataGeneratorJob> getRepoJobs() {
    return repoJobs;
  }

}
