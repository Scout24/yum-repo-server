package de.is24.infrastructure.gridfs.http.mongo;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class MongoPrimaryDetectorIT {

  @ClassRule
  public static IntegrationTestContext context = new IntegrationTestContext();
  private MongoPrimaryDetector detector;

  @Before
  public void setUp() throws Exception {
    detector = new MongoPrimaryDetector(context.getMongo());
  }

  @Test
  public void detectPrimary() throws Exception {
    assertThat(detector.isPrimary(), is(true));
  }
}
