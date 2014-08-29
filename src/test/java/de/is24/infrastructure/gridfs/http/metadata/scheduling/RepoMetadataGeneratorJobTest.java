package de.is24.infrastructure.gridfs.http.metadata.scheduling;

import de.is24.infrastructure.gridfs.http.metadata.MetadataService;
import de.is24.infrastructure.gridfs.http.mongo.MongoPrimaryDetector;
import org.junit.Before;
import org.junit.Test;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class RepoMetadataGeneratorJobTest {
  private static final String REPO_NAME = "any-repo";
  private static final int DELAY = 12;

  private MetadataService service;
  private MongoPrimaryDetector detector;
  private RepoMetadataGeneratorJob job;
  private ScheduledExecutorService scheduledExecutorService;
  private ScheduledFuture<?> scheduledFuture;


  @Before
  @SuppressWarnings("unchecked")
  public void setUp() throws Exception {
    service = mock(MetadataService.class);
    detector = mock(MongoPrimaryDetector.class);
    scheduledFuture = mock(ScheduledFuture.class);
    scheduledExecutorService = mock(ScheduledExecutorService.class);
    doReturn(scheduledFuture).when(scheduledExecutorService)
    .scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
    job = new RepoMetadataGeneratorJob(REPO_NAME, service, detector, scheduledExecutorService, DELAY);
  }

  @Test
  public void executeMetadataGenerationIfPrimaryAndActive() throws Exception {
    when(detector.isPrimary()).thenReturn(true);
    job.run();
    verify(service).generateYumMetadataIfNecessary(eq(REPO_NAME));
  }

  @Test
  public void doNothingIfNotPrimary() throws Exception {
    when(detector.isPrimary()).thenReturn(false);
    job.run();
    verify(service, never()).generateYumMetadataIfNecessary(eq(REPO_NAME));
  }

  @Test
  public void doNothingIfNotActive() throws Exception {
    when(detector.isPrimary()).thenReturn(true);
    job.deactivate();
    job.run();
    verify(service, never()).generateYumMetadataIfNecessary(eq(REPO_NAME));
  }

  @Test
  public void registerItselfAsDelayedScheduledTask() throws Exception {
    verify(scheduledExecutorService).scheduleWithFixedDelay(eq(job),
      eq((long) DELAY),
      eq((long) DELAY),
      eq(TimeUnit.SECONDS));
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
