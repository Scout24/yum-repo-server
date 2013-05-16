package de.is24.infrastructure.gridfs.http.mongo;

import com.mongodb.Mongo;
import com.mongodb.ReplicaSetStatus;
import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class MongoPrimaryDetectorTest {
  private Mongo mongoMock;
  private MongoPrimaryDetector primaryDetector;

  @Before
  public void setUp() throws Exception {
    mongoMock = mock(Mongo.class);
    primaryDetector = new MongoPrimaryDetector(mongoMock);
  }

  @Test
  public void ifThereIsNoMasterReturnFalse() throws Exception {
    when(mongoMock.getReplicaSetStatus()).thenReturn(mock(ReplicaSetStatus.class));

    assertThat(primaryDetector.isPrimary(), is(false));
  }
}
