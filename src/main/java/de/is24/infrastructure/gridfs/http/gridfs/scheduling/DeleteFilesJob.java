package de.is24.infrastructure.gridfs.http.gridfs.scheduling;

import de.is24.infrastructure.gridfs.http.gridfs.GridFsService;
import de.is24.infrastructure.gridfs.http.mongo.MongoPrimaryDetector;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;


@ManagedResource
@Service
public class DeleteFilesJob {
  private final GridFsService gridFsService;
  private final MongoPrimaryDetector primaryDetector;
  private final int minuetsToWaitForActualDelete;

  private AtomicLong executionsSinceStartUp = new AtomicLong();
  private AtomicLong failureSinceStartUp = new AtomicLong();

  private String lastStackTrace;

  @Autowired
  public DeleteFilesJob(final GridFsService gridFsService,
                        final MongoPrimaryDetector primaryDetector,
                        @Value("${scheduler.delete.files.delay.minuets:10}") final int minuetsToWaitForActualDelete) {
    this.gridFsService = gridFsService;
    this.primaryDetector = primaryDetector;
    this.minuetsToWaitForActualDelete = minuetsToWaitForActualDelete;
  }


  @Scheduled(cron = "${scheduler.delete.files.cron:0 3-59/15 * * * *}")
  public void deleteFilesMarkedAsDeleted() {
    deleteFilesMarkedAsDeleted(new Date());
  }

  @ManagedOperation
  public void deleteFilesMarkedAsDeletedNow() {
    doRemoveFilesMarkedAsDeleted(new Date());
  }

  @ManagedAttribute
  public long getExecutionsSinceStartup() {
    return executionsSinceStartUp.get();
  }

  @ManagedAttribute
  public long getFailureSinceStartUp() {
    return failureSinceStartUp.get();
  }

  @ManagedAttribute
  public String getLastStackTrace() {
    return lastStackTrace;
  }

  //just for easier testing
  void deleteFilesMarkedAsDeleted(final Date now) {
    if (primaryDetector.isPrimary()) {
      doRemoveFilesMarkedAsDeleted(now);
    }
  }

  private void doRemoveFilesMarkedAsDeleted(Date now) {
    executionsSinceStartUp.incrementAndGet();
    try {
      gridFsService.removeFilesMarkedAsDeletedBefore(DateUtils.addMinutes(now, -minuetsToWaitForActualDelete));
    } catch (Exception ex) {
      failureSinceStartUp.incrementAndGet();
      lastStackTrace = ExceptionUtils.getFullStackTrace(ex);
      throw ex;
    }
  }
}
