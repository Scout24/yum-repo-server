package de.is24.infrastructure.gridfs.http.utils.retry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@ContextConfiguration(classes = RetryAspectTestContext.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class RetryAspectTest {
  @Autowired
  RetryAspectTestComponent retryAspectTestComponent;

  @Test
  public void shouldRetry() throws Exception {
    try {
      retryAspectTestComponent.methodWithRetry();
    } catch (Exception e) {
    } finally {
      assertThat(retryAspectTestComponent.getTimesCalled(), is(3));
    }
  }

}
