package de.is24.infrastructure.gridfs.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import de.is24.util.monitoring.tools.DoNothingReportVisitor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import de.is24.util.monitoring.InApplicationMonitor;
import de.is24.util.monitoring.StateValueProvider;


public class AppConfigTest {
  private AppConfig appConfig;
  private ThreadPoolTaskScheduler taskScheduler;

  @Before
  public void setUp() {
    appConfig = new AppConfig();
    appConfig.schedulerPoolSize = 1;

    taskScheduler = appConfig.taskScheduler();
    taskScheduler.initialize();
  }

  @After
  public void tearDown() {
    taskScheduler.shutdown();
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
