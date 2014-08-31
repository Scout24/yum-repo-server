package de.is24.infrastructure.gridfs.http;

import de.is24.infrastructure.gridfs.http.security.AuthenticationDetails;
import de.is24.infrastructure.gridfs.http.security.UserAuthorities;
import de.is24.util.monitoring.InApplicationMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.concurrent.DelegatingSecurityContextScheduledExecutorService;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Configuration
@EnableScheduling
public class SchedulingConfig {

  private static final String METADATA_SCHEDULER = "metadata.scheduler";

  @Value("${scheduler.poolSize:10}")
  int schedulerPoolSize;

  @Autowired
  InApplicationMonitor inApplicationMonitor;

  @Bean
  public ScheduledExecutorService scheduledExecutorService() {
    ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(schedulerPoolSize);
    setupMonitorForQueueSize(scheduledThreadPoolExecutor);
    return new DelegatingSecurityContextScheduledExecutorService(scheduledThreadPoolExecutor, getSecurityContext());
  }

  public SecurityContext getSecurityContext() {
    SecurityContext context = SecurityContextHolder.getContext();
    PreAuthenticatedAuthenticationToken authentication = new PreAuthenticatedAuthenticationToken(
        METADATA_SCHEDULER + ".user",
        "no credentials",
        UserAuthorities.USER_AUTHORITIES);
    authentication.setDetails(new AuthenticationDetails());
    context.setAuthentication(
        authentication);
    return context;
  }

  private void setupMonitorForQueueSize(final ScheduledThreadPoolExecutor scheduler) {
    inApplicationMonitor.registerStateValue(new QueueSizeValueProvider(scheduler, METADATA_SCHEDULER));
  }
}
