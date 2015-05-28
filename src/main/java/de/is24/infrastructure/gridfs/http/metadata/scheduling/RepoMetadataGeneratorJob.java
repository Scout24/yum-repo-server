package de.is24.infrastructure.gridfs.http.metadata.scheduling;

import de.is24.infrastructure.gridfs.http.metadata.MetadataService;
import de.is24.infrastructure.gridfs.http.mongo.MongoPrimaryDetector;
import de.is24.infrastructure.gridfs.http.utils.MDCHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang.builder.HashCodeBuilder.reflectionHashCode;
import static org.apache.commons.lang.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang.builder.ToStringStyle.SHORT_PREFIX_STYLE;


public class RepoMetadataGeneratorJob implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(RepoMetadataGeneratorJob.class);

  private final String name;
  private final MetadataService metadataService;
  private final MongoPrimaryDetector primaryDetector;
  private final ScheduledFuture<?> scheduledFuture;
  private boolean active = true;

  @SuppressWarnings("unchecked")
  public RepoMetadataGeneratorJob(String name, MetadataService metadataService, MongoPrimaryDetector primaryDetector,
                                  ScheduledExecutorService scheduledExecutorService, int delayInSec) {
    this.name = name;
    this.metadataService = metadataService;
    this.primaryDetector = primaryDetector;
    this.scheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(this, delayInSec, delayInSec, SECONDS);
  }

  public String getName() {
    return name;
  }

  @Override
  public void run() {
    if (!active) {
      LOG.debug("Skipping generation for repository {} because not active.", name);
      return;
    }

    if (!primaryDetector.isPrimary()) {
      LOG.debug("Skipping generation for repository {} because not connected to mongo primary.", name);
      return;
    }

    doRun();
  }

  private void doRun() {
    LOG.debug("Scheduled generation for repository: {}", name);
    new MDCHelper(this.getClass()).run(() -> {
      try {
        metadataService.generateYumMetadataIfNecessary(name);
      } catch (SQLException | IOException e) {
        LOG.error("Metadata generation for repository {} failed.", name, e);
      }
    });
  }

  @Override
  public boolean equals(Object o) {
    return reflectionEquals(this, o);
  }

  @Override
  public int hashCode() {
    return reflectionHashCode(this);
  }

  @Override
  public String toString() {
    return reflectionToString(this, SHORT_PREFIX_STYLE);
  }

  public void deactivate() {
    if (active) {
      this.active = false;
      scheduledFuture.cancel(false);
    }
  }

  public boolean isActive() {
    return active;
  }

  /**
   * return true if this job is not actively scheduled any longer. This state might change to true if exception
   * is encountered while processing this job
   * @return true if this job will not any longer be scheduled
   */
  public boolean isNotScheduledAnyLonger() {
    return scheduledFuture.isDone();
  }
}
