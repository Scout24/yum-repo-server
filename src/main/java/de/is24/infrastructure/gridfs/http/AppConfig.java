package de.is24.infrastructure.gridfs.http;

import de.is24.infrastructure.gridfs.http.mongo.MongoConfig;
import de.is24.infrastructure.gridfs.http.security.MethodSecurityConfig;
import de.is24.infrastructure.gridfs.http.security.WebSecurityConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

@ComponentScan(basePackages = { "de.is24", "org.springframework.data.mongodb.tx" })
@Configuration
@EnableAspectJAutoProxy
@Import({ PropertyConfig.class, MongoConfig.class, SchedulingConfig.class, WebSecurityConfig.class, MethodSecurityConfig.class, MonitoringConfig.class })
public class AppConfig {

  @Bean
  public StandardServletMultipartResolver multipartResolver() {
    return new StandardServletMultipartResolver();
  }
}
