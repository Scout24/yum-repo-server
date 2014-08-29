package de.is24.infrastructure.gridfs.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import de.is24.util.monitoring.tools.DoNothingReportVisitor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import de.is24.util.monitoring.InApplicationMonitor;
import de.is24.util.monitoring.StateValueProvider;
import java.util.concurrent.ScheduledExecutorService;


public class AppConfigTest {
  private AppConfig appConfig;
  private ScheduledExecutorService scheduledExecutorService;

  @Before
  public void setUp() {
    appConfig = new AppConfig();
    appConfig.schedulerPoolSize = 1;

    scheduledExecutorService = appConfig.scheduledExecutorService();
  }

  @After
  public void tearDown() {
    scheduledExecutorService.shutdown();
  }

  @Test
  public void taskSchedulerRegistersQueueSizeValueProvider() throws Exception {
    QueueSizeVisitor queueSizeVisitor = new QueueSizeVisitor();
    InApplicationMonitor.getInstance().getCorePlugin().reportInto(queueSizeVisitor);
    assertThat(queueSizeVisitor.knowsQueueSizeState(), is(true));
  }

  private final class QueueSizeVisitor extends DoNothingReportVisitor {
    private boolean knowsQueueSizeState = false;


    public boolean knowsQueueSizeState() {
      return knowsQueueSizeState;
    }


    @Override
    public void reportStateValue(StateValueProvider stateValueProvider) {
      if ("metadata.scheduler.queueSize".equals(stateValueProvider.getName())) {
        knowsQueueSizeState = true;
      }
    }

  }

}
