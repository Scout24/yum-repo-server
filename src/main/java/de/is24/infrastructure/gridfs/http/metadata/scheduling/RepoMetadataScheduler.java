package de.is24.infrastructure.gridfs.http.metadata.scheduling;

import de.is24.infrastructure.gridfs.http.domain.RepoEntry;
import de.is24.infrastructure.gridfs.http.metadata.MetadataService;
import de.is24.infrastructure.gridfs.http.metadata.RepoEntriesRepository;
import de.is24.infrastructure.gridfs.http.mongo.MongoPrimaryDetector;
import de.is24.infrastructure.gridfs.http.utils.MDCHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import static de.is24.infrastructure.gridfs.http.domain.RepoType.SCHEDULED;
import static java.util.stream.Collectors.toSet;


@ManagedResource
@Service
public class RepoMetadataScheduler {
  private static final Logger LOG = LoggerFactory.getLogger(RepoMetadataScheduler.class);

  private final RepoEntriesRepository repo;
  private final MetadataService metadataService;
  private final MongoPrimaryDetector primaryDetector;
  private final ScheduledExecutorService scheduledExecutorService;
  private final int delayInSec;
  private Map<String, RepoMetadataGeneratorJob> repoJobs;

  @Autowired
  public RepoMetadataScheduler(RepoEntriesRepository repo,
                               MetadataService metadataService,
                               MongoPrimaryDetector primaryDetector,
                               ScheduledExecutorService scheduledExecutorService,
                               @Value("${scheduler.delay:10}") int delayInSec) {
    this.repo = repo;
    this.metadataService = metadataService;
    this.primaryDetector = primaryDetector;
    this.scheduledExecutorService = scheduledExecutorService;
    this.delayInSec = delayInSec;
    this.repoJobs = new HashMap<>();
  }

  @Scheduled(cron = "${scheduler.update.cron:*/30 * * * * *}")
  public void update() {
    LOG.debug("Checking for updates in scheduled repository definitions.");
    new MDCHelper(this.getClass()).run(() -> {
      try {
        Set<String> repoNamesToSchedule = repo.findByType(SCHEDULED).stream().map(RepoEntry::getName).collect(toSet());
        repoNamesToSchedule.forEach(this::createRepoJob);
        removeJobsNotFoundInDb(repoNamesToSchedule);
      } catch (Exception e) {
        LOG.error(e.getMessage());
      }
    });
  }

  private void removeJobsNotFoundInDb(Set<String> repoNamesToSchedule) {
    repoJobs.keySet().stream().filter(repoName -> !repoNamesToSchedule.contains(repoName)).forEach(this::removeRepoJob);
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
        new RepoMetadataGeneratorJob(name, metadataService, primaryDetector, scheduledExecutorService, delayInSec));
      LOG.info("Added scheduling job for repository: {}", name);
    }
  }

  @ManagedOperation
  public Map<String, RepoMetadataGeneratorJob> getRepoJobs() {
    return repoJobs;
  }

}
