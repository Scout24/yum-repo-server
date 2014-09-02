package de.is24.infrastructure.gridfs.http.monitoring;

import de.is24.util.monitoring.StateValueProvider;
import java.util.concurrent.ScheduledThreadPoolExecutor;


public final class PoolSizeValueProvider extends StateValueProvider {
  private final ScheduledThreadPoolExecutor executor;
  private String queueName;

  public PoolSizeValueProvider(ScheduledThreadPoolExecutor executor, String queueName) {
    this.executor = executor;
    this.queueName = queueName;
  }

  @Override
  public long getValue() {
    return executor.getPoolSize();
  }

  @Override
  public String getName() {
    return queueName + ".poolSize";
  }
}
