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
import static ch.lambdaj.Lambda.extract;
import static ch.lambdaj.Lambda.on;
import static de.is24.infrastructure.gridfs.http.domain.RepoType.SCHEDULED;


@ManagedResource
@Service
public class RepoMetadataSchedulerJob {
  private static final Logger LOG = LoggerFactory.getLogger(RepoMetadataSchedulerJob.class);

  private final RepoEntriesRepository repo;
  private final MetadataService metadataService;
  private final MongoPrimaryDetector primaryDetector;
  private final TaskScheduler taskScheduler;
  private final int delayInSec;
  private Map<String, RepoMetadataGeneratorJob> repoJobs;

  @Autowired
  public RepoMetadataSchedulerJob(RepoEntriesRepository repo,
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

    Set<String> repoNamesToSchedule = new HashSet<>(extract(repo.findByType(SCHEDULED), on(RepoEntry.class).getName()));
    for (String repoToSchedule : repoNamesToSchedule) {
      createRepoJob(repoToSchedule);
    }

    removeJobsNotFoundInDb(repoNamesToSchedule);
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
