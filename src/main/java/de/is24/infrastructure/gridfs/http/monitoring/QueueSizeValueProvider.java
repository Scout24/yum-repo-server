package de.is24.infrastructure.gridfs.http.monitoring;

import de.is24.util.monitoring.StateValueProvider;
import java.util.concurrent.ScheduledThreadPoolExecutor;


public final class QueueSizeValueProvider extends StateValueProvider {
  private final ScheduledThreadPoolExecutor executor;
  private String queueName;

  public QueueSizeValueProvider(ScheduledThreadPoolExecutor executor, String queueName) {
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
