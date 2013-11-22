package de.is24.infrastructure.gridfs.http.monitoring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;


@Configuration
@EnableAspectJAutoProxy
public class TimeMeasurementAspectTestContext {
  @Bean
  public ClassAnnotated classAnnotated() {
    return new ClassAnnotated();
  }

  @Bean
  public MethodAnnotated methodAnnotated() {
    return new MethodAnnotated();
  }

  @Bean
  public TimeMeasurementAspect timeMeasurementAspect() {
    return new TimeMeasurementAspect();
  }
}
