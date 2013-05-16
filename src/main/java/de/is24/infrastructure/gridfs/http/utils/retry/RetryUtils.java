package de.is24.infrastructure.gridfs.http.utils.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static java.lang.String.format;
import static java.lang.Thread.sleep;


/**
 * Alternatively, check out: http://code.google.com/p/java-retryer/
 */
public final class RetryUtils {
  private static final Logger LOG = LoggerFactory.getLogger(RetryUtils.class);

  private int maxTries;
  private int sleepInSec;

  private RetryUtils() {
  }

  public static MaxRetriesConfig execute() {
    RetryUtils retryUtils = new RetryUtils();
    return retryUtils.new MaxRetriesConfig();
  }

  public final class MaxRetriesConfig {
    private MaxRetriesConfig() {
    }

    public WaitConfig maxTries(int maxRetries) {
      RetryUtils.this.maxTries = maxRetries;
      return new WaitConfig();
    }
  }

  public final class WaitConfig {
    private WaitConfig() {
    }

    public RetryCommand wait(int sleepInSecs) {
      RetryUtils.this.sleepInSec = sleepInSecs;
      return new RetryCommand();
    }
  }

  public interface Retryable<T> {
    T run() throws Throwable;
  }

  public final class RetryCommand {
    private RetryCommand() {
    }

    public <T> T command(Retryable<T> command) throws Throwable {
      T result = null;
      for (int i = 0; i < maxTries; i++) {
        try {
          result = command.run();
          break;
        } catch (Throwable e) {
          int lastTry = maxTries - 1;
          if (i < lastTry) {
            LOG.warn(format("Execution %s of %s failed. Retry after %s seconds.", i + 1, maxTries, sleepInSec), e);
            sleep(sleepInSec * 1000);
          } else {
            LOG.error(format("Execution %s of %s failed. No retries left.", i + 1, maxTries));
            throw e;
          }
        }
      }
      return result;
    }
  }

}
