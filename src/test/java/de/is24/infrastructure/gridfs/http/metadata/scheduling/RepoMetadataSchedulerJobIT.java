package de.is24.infrastructure.gridfs.http.metadata.scheduling;

import static de.is24.infrastructure.gridfs.http.domain.RepoType.SCHEDULED;
import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.uniqueRepoName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.UnknownHostException;
import java.util.concurrent.ScheduledFuture;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.scheduling.TaskScheduler;
import de.is24.infrastructure.gridfs.http.domain.RepoEntry;
import de.is24.infrastructure.gridfs.http.metadata.MetadataService;
import de.is24.infrastructure.gridfs.http.metadata.generation.RepoMdGenerator;
import de.is24.infrastructure.gridfs.http.mongo.IntegrationTestContext;
import de.is24.infrastructure.gridfs.http.mongo.MongoPrimaryDetector;


public class RepoMetadataSchedulerJobIT {
  public static final int DELAY = 10;

  private RepoMetadataSchedulerJob schedulerJob;

  @ClassRule
  public static IntegrationTestContext context = new IntegrationTestContext();

  @Before
  public void setUp() throws UnknownHostException {
    RepoMdGenerator repoMdGenerator = new RepoMdGenerator(context.gridFs());
    MetadataService metadataService = new MetadataService(context.gridFsService(), context.yumEntriesRepository(),
      repoMdGenerator, context.repoService(), context.repoCleaner());
    MongoPrimaryDetector primaryDetector = new MongoPrimaryDetector(context.getMongo());
    ScheduledFuture<Void> scheduledFuture = mock(ScheduledFuture.class);
    TaskScheduler taskScheduler = mock(TaskScheduler.class);
    when(taskScheduler.scheduleWithFixedDelay(any(Runnable.class), anyLong())).thenReturn(scheduledFuture);
    schedulerJob = new RepoMetadataSchedulerJob(context.repoEntriesRepository(), metadataService, primaryDetector,
      taskScheduler,
      DELAY);
  }

  @Test
  public void createNewJobForConfiguredRepos() throws Exception {
    String repoName = givenSchedulerWithOneRunningJob();
    RepoMetadataGeneratorJob job = schedulerJob.getRepoJobs().get(repoName);
    assertThat(job, notNullValue());
  }

  @Test
  public void doNotCreateJobIfAlreadyExists() throws Exception {
    String repoName = givenSchedulerWithOneRunningJob();

    RepoMetadataGeneratorJob jobBeforeSecondUpdate = schedulerJob.getRepoJobs().get(repoName);
    schedulerJob.update();

    RepoMetadataGeneratorJob jobAfterSecondUpdate = schedulerJob.getRepoJobs().get(repoName);

    assertThat(jobBeforeSecondUpdate, sameInstance(jobAfterSecondUpdate));
  }

  @Test
  public void removeJobAfterRemovedFromDB() throws Exception {
    String repoName = givenSchedulerWithOneRunningJob();
    context.repoEntriesRepository().delete(context.repoEntriesRepository().findFirstByName(repoName).getId());

    RepoMetadataGeneratorJob existingJob = schedulerJob.getRepoJobs().get(repoName);
    assertThat(existingJob.isActive(), is(true));

    schedulerJob.update();
    assertThat(schedulerJob.getRepoJobs().get(repoName), nullValue());
    assertThat(existingJob.isActive(), is(false));
  }

  private String givenSchedulerWithOneRunningJob() {
    String repoName = uniqueRepoName();
    RepoEntry entry = new RepoEntry();
    entry.setName(repoName);
    entry.setType(SCHEDULED);
    context.repoEntriesRepository().save(entry);
    schedulerJob.update();
    return repoName;
  }

}
