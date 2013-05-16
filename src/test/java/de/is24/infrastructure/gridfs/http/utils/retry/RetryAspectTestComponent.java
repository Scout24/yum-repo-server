package de.is24.infrastructure.gridfs.http.utils.retry;

import de.is24.infrastructure.gridfs.http.utils.retry.Retry;


public class RetryAspectTestComponent {
  private int timesCalled = 0;

  @Retry(maxTries = 3, secondsToWait = 0)
  public void methodWithRetry() {
    timesCalled++;
    throw new RuntimeException("Expected test exception");
  }

  public int getTimesCalled() {
    return timesCalled;
  }

}
