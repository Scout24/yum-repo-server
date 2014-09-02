package de.is24.infrastructure.gridfs.http.monitoring;

import de.is24.util.monitoring.StateValueProvider;
import java.util.concurrent.ScheduledThreadPoolExecutor;


public final class ActiveJobsValueProvider extends StateValueProvider {
  private final ScheduledThreadPoolExecutor executor;
  private String queueName;

  public ActiveJobsValueProvider(ScheduledThreadPoolExecutor executor, String queueName) {
    this.executor = executor;
    this.queueName = queueName;
  }

  @Override
  public long getValue() {
    return executor.getActiveCount();
  }

  @Override
  public String getName() {
    return queueName + ".activeCount";
  }
}
