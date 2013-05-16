package de.is24.infrastructure.gridfs.http;

import de.is24.util.monitoring.StateValueProvider;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import java.util.concurrent.ScheduledThreadPoolExecutor;


final class QueueSizeValueProvider extends StateValueProvider {
  private final ThreadPoolTaskScheduler scheduler;

  QueueSizeValueProvider(ThreadPoolTaskScheduler scheduler) {
    this.scheduler = scheduler;
  }

  @Override
  public long getValue() {
    final ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) scheduler.getScheduledExecutor();
    return executor.getQueue().size();
  }

  @Override
  public String getName() {
    return scheduler.getThreadGroup().getName() + ".queueSize";
  }
}
