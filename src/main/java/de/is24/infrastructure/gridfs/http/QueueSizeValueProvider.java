package de.is24.infrastructure.gridfs.http;

import de.is24.util.monitoring.StateValueProvider;
import java.util.concurrent.ScheduledThreadPoolExecutor;


final class QueueSizeValueProvider extends StateValueProvider {
  private final ScheduledThreadPoolExecutor executor;
  private String queueName;

  QueueSizeValueProvider(ScheduledThreadPoolExecutor executor, String queueName) {
    this.executor = executor;
    this.queueName = queueName;
  }

  @Override
  public long getValue() {
    return executor.getQueue().size();
  }

  @Override
  public String getName() {
    return queueName + ".queueSize";
  }
}
