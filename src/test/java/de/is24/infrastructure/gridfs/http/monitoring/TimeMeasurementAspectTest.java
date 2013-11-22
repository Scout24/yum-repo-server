package de.is24.infrastructure.gridfs.http.monitoring;

import de.is24.util.monitoring.InApplicationMonitor;
import de.is24.util.monitoring.MonitorPlugin;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@ContextConfiguration(classes = TimeMeasurementAspectTestContext.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class TimeMeasurementAspectTest {
  @Before
  public void setup() {
    InApplicationMonitor.getInstance().removeAllPlugins();
  }

  @After
  public void cleanup() {
    InApplicationMonitor.getInstance().removeAllPlugins();
  }

  @Autowired
  private ClassAnnotated classAnnotated;

  @Autowired
  private MethodAnnotated methodAnnotated;


  @Test
  public void methodAnnotatedTimerMeasurement() throws Exception {
    MonitorPlugin monitorPlugin = mock(MonitorPlugin.class);
    InApplicationMonitor.getInstance().registerPlugin(monitorPlugin);

    methodAnnotated.methodOne();
    methodAnnotated.methodTwo("lala");

    verify(monitorPlugin, times(1)).addTimerMeasurement(eq(
        "de.is24.infrastructure.gridfs.http.monitoring.MethodAnnotated.methodOne"),
      anyLong());

    verify(monitorPlugin, times(1)).addTimerMeasurement(eq(
        "de.is24.infrastructure.gridfs.http.monitoring.MethodAnnotated.methodTwo"),
      anyLong());

  }

  @Test
  public void classAnnotatedTimerMeasurement() throws Exception {
    MonitorPlugin monitorPlugin = mock(MonitorPlugin.class);
    InApplicationMonitor.getInstance().registerPlugin(monitorPlugin);

    classAnnotated.methodOne();
    classAnnotated.methodTwo("lala");

    verify(monitorPlugin, times(1)).addTimerMeasurement(eq(
        "de.is24.infrastructure.gridfs.http.monitoring.ClassAnnotated.methodOne"),
      anyLong());

    verify(monitorPlugin, times(1)).addTimerMeasurement(eq(
        "de.is24.infrastructure.gridfs.http.monitoring.ClassAnnotated.methodTwo"),
      anyLong());

  }
}
