package de.is24.infrastructure.gridfs.http.utils.retry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import de.is24.infrastructure.gridfs.http.utils.retry.RetryAspect;


@Configuration
@EnableAspectJAutoProxy
public class RetryAspectTestContext {
  @Bean
  public RetryAspectTestComponent testComponentUsingRetry() {
    return new RetryAspectTestComponent();
  }

  @Bean
  public RetryAspect retryAspect() {
    return new RetryAspect();
  }
}
