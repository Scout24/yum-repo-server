package de.is24.infrastructure.gridfs.http.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.Test;

import de.is24.infrastructure.gridfs.http.utils.retry.RetryUtils;
import de.is24.infrastructure.gridfs.http.utils.retry.RetryUtils.Retryable;


public class RetryUtilsTest {
  @Test
  public void shouldRetry() throws Throwable {
    @SuppressWarnings("unchecked")
    Retryable<Void> retryable = mock(Retryable.class);
    when(retryable.run()).thenThrow(new Exception("Test Exception!")).thenReturn(null);

    RetryUtils.execute().maxTries(2).wait(0).command(retryable);

    verify(retryable, times(2)).run();
  }

  @Test
  public void shouldStopRetryAfterMaxTries() throws Throwable {
    @SuppressWarnings("unchecked")
    Retryable<Void> retryable = mock(Retryable.class);
    Exception givenException = new Exception("Test Exception!");
    when(retryable.run()).thenThrow(givenException).thenThrow(givenException);

    try {
      RetryUtils.execute().maxTries(1).wait(0).command(retryable);
    } catch (Exception e) {
      assertThat(e, is(givenException));
    } finally {
      verify(retryable, times(1)).run();
    }
  }

}
