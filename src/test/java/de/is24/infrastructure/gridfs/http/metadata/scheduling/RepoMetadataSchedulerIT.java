package de.is24.infrastructure.gridfs.http.metadata.scheduling;

import de.is24.infrastructure.gridfs.http.domain.RepoEntry;
import de.is24.infrastructure.gridfs.http.metadata.MetadataService;
import de.is24.infrastructure.gridfs.http.mongo.IntegrationTestContext;
import de.is24.infrastructure.gridfs.http.mongo.MongoPrimaryDetector;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import java.net.UnknownHostException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import static de.is24.infrastructure.gridfs.http.domain.RepoType.SCHEDULED;
import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.uniqueRepoName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;


public class RepoMetadataSchedulerIT {
  public static final int DELAY = 10;

  private RepoMetadataScheduler metadataScheduler;
  private ScheduledFuture<?> scheduledFuture;

  @ClassRule
  public static IntegrationTestContext context = new IntegrationTestContext();

  @Before
  @SuppressWarnings("unchecked")
  public void setUp() throws UnknownHostException {
    MetadataService metadataService = context.metadataService();
    MongoPrimaryDetector primaryDetector = new MongoPrimaryDetector(context.getMongo());
    ScheduledExecutorService scheduledExecutorService = mock(ScheduledExecutorService.class);
    scheduledFuture = mock(ScheduledFuture.class);
    doReturn(scheduledFuture).when(scheduledExecutorService)
    .scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));

    metadataScheduler = new RepoMetadataScheduler(context.repoEntriesRepository(),
      metadataService,
      primaryDetector,
      scheduledExecutorService,
      DELAY);
  }


  @Test
  public void createNewJobForConfiguredRepos() throws Exception {
    String repoName = givenSchedulerWithOneRunningJob();
    RepoMetadataGeneratorJob job = metadataScheduler.getRepoJobs().get(repoName);
    assertThat(job, notNullValue());
  }

  @Test
  public void doNotCreateJobIfAlreadyExists() throws Exception {
    String repoName = givenSchedulerWithOneRunningJob();

    RepoMetadataGeneratorJob jobBeforeSecondUpdate = metadataScheduler.getRepoJobs().get(repoName);
    metadataScheduler.update();

    RepoMetadataGeneratorJob jobAfterSecondUpdate = metadataScheduler.getRepoJobs().get(repoName);

    assertThat(jobBeforeSecondUpdate, sameInstance(jobAfterSecondUpdate));
  }

  @Test
  // reproduces github issue #59 internal issue DATA-4843
  public void replaceJobsNotScheduledAnyLonger() throws Exception {
    givenFailingFutureMock();

    String repoName = givenSchedulerWithOneRunningJob();

    RepoMetadataGeneratorJob jobBeforeSecondUpdate = metadataScheduler.getRepoJobs().get(repoName);
    metadataScheduler.update();

    RepoMetadataGeneratorJob jobAfterSecondUpdate = metadataScheduler.getRepoJobs().get(repoName);

    assertThat(jobBeforeSecondUpdate, not(sameInstance(jobAfterSecondUpdate)));
  }


  @Test
  public void removeJobAfterRemovedFromDB() throws Exception {
    String repoName = givenSchedulerWithOneRunningJob();
    context.repoEntriesRepository().delete(context.repoEntriesRepository().findFirstByName(repoName).getId());

    RepoMetadataGeneratorJob existingJob = metadataScheduler.getRepoJobs().get(repoName);
    assertThat(existingJob.isActive(), is(true));

    metadataScheduler.update();
    assertThat(metadataScheduler.getRepoJobs().get(repoName), nullValue());
    assertThat(existingJob.isActive(), is(false));
  }

  private String givenSchedulerWithOneRunningJob() {
    String repoName = uniqueRepoName();
    RepoEntry entry = new RepoEntry();
    entry.setName(repoName);
    entry.setType(SCHEDULED);
    context.repoEntriesRepository().save(entry);
    metadataScheduler.update();
    return repoName;
  }


  private void givenFailingFutureMock() {
    doReturn(true).when(scheduledFuture).isDone();

  }

}
