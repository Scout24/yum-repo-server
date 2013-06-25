package de.is24.infrastructure.gridfs.http.gridfs.scheduling;

import de.is24.infrastructure.gridfs.http.gridfs.GridFsService;
import de.is24.infrastructure.gridfs.http.mongo.MongoPrimaryDetector;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import java.util.Date;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class DeleteFilesJobTest {
  public static final int CONFIGURED_WAIT = 6;
  @Mock
  private MongoPrimaryDetector primaryDetectorMock;

  @Mock
  private GridFsService gridFsServiceMock;

  private DeleteFilesJob deleteFilesJob;

  private final Date testStart = new Date();

  @Before
  public void setUp() throws Exception {
    deleteFilesJob = new DeleteFilesJob(gridFsServiceMock, primaryDetectorMock, CONFIGURED_WAIT);
  }

  @Test
  public void doNothingIfNotPrimary() throws Exception {
    when(primaryDetectorMock.isPrimary()).thenReturn(false);

    deleteFilesJob.deleteFilesMarkedAsDeleted(testStart);

    verifyZeroInteractions(gridFsServiceMock);
  }

  @Test
  public void deleteFilesBeforeConfiguredWaitOnPrimary() throws Exception {
    when(primaryDetectorMock.isPrimary()).thenReturn(true);

    deleteFilesJob.deleteFilesMarkedAsDeleted(testStart);

    verify(gridFsServiceMock).removeFilesMarkedAsDeletedBefore(DateUtils.addMinutes(testStart, -CONFIGURED_WAIT));
  }
}
