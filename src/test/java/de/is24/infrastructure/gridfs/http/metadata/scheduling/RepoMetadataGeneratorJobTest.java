package de.is24.infrastructure.gridfs.http.metadata.scheduling;

import de.is24.infrastructure.gridfs.http.metadata.MetadataService;
import de.is24.infrastructure.gridfs.http.mongo.MongoPrimaryDetector;
import org.junit.Before;
import org.junit.Test;
import org.springframework.scheduling.TaskScheduler;
import java.util.concurrent.ScheduledFuture;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class RepoMetadataGeneratorJobTest {
  private static final String REPO_NAME = "any-repo";
  private static final int DELAY = 12;
  private static final long DELAY_IN_MS = DELAY * 1000;

  private MetadataService service;
  private MongoPrimaryDetector detector;
  private RepoMetadataGeneratorJob job;
  private TaskScheduler taskScheduler;
  private ScheduledFuture<Void> scheduledFuture;


  @Before
  @SuppressWarnings("unchecked")
  public void setUp() throws Exception {
    service = mock(MetadataService.class);
    detector = mock(MongoPrimaryDetector.class);
    scheduledFuture = mock(ScheduledFuture.class);
    taskScheduler = mock(TaskScheduler.class);
    when(taskScheduler.scheduleWithFixedDelay(any(Runnable.class), anyLong())).thenReturn(scheduledFuture);
    job = new RepoMetadataGeneratorJob(REPO_NAME, service, detector, taskScheduler, DELAY);
  }

  @Test
  public void executeMetadataGenerationIfPrimaryAndActive() throws Exception {
    when(detector.isPrimary()).thenReturn(true);
    job.run();
    verify(service).generateYumMetadata(eq(REPO_NAME));
  }

  @Test
  public void doNothingIfNotPrimary() throws Exception {
    when(detector.isPrimary()).thenReturn(false);
    job.run();
    verify(service, never()).generateYumMetadata(eq(REPO_NAME));
  }

  @Test
  public void doNothingIfNotActive() throws Exception {
    when(detector.isPrimary()).thenReturn(true);
    job.deactivate();
    job.run();
    verify(service, never()).generateYumMetadata(eq(REPO_NAME));
  }

  @Test
  public void registerItselfAsDelayedScheduledTask() throws Exception {
    verify(taskScheduler).scheduleWithFixedDelay(eq(job), eq(DELAY_IN_MS));
  }

  @Test
  public void unregisterTaskAfterDeactivation() throws Exception {
    job.deactivate();
    verify(scheduledFuture).cancel(eq(false));
  }

  @Test
  public void canDeactivateMoreThanOnce() throws Exception {
    job.deactivate();
    job.deactivate();
    job.deactivate();
    verify(scheduledFuture).cancel(eq(false));
  }
}
