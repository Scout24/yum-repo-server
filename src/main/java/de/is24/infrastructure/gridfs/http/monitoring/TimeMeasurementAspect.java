package de.is24.infrastructure.gridfs.http.monitoring;

import de.is24.util.monitoring.InApplicationMonitor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import static java.lang.System.currentTimeMillis;


@Aspect
@Component
public class TimeMeasurementAspect {
  @Pointcut("@annotation(de.is24.infrastructure.gridfs.http.monitoring.TimeMeasurement)")
  private void timeMeasurementMethods() {
  }

  @Pointcut("@within(de.is24.infrastructure.gridfs.http.monitoring.TimeMeasurement)")
  private void timeMeasurementTypes() {
  }

  @Pointcut("execution(public * *(..))")
  private void publicMethods() {
  }

  @Pointcut("timeMeasurementTypes() && publicMethods()")
  private void publicMethodsInTimeMeasuredTypes() {
  }

  @Around("timeMeasurementMethods() || publicMethodsInTimeMeasuredTypes()")
  public Object doBasicProfiling(ProceedingJoinPoint pjp) throws Throwable {
    long startTime = currentTimeMillis();
    try {
      return pjp.proceed();
    } finally {
      long endTime = currentTimeMillis();
      String name = pjp.getSignature().getDeclaringTypeName() + "." + pjp.getSignature().getName();
      InApplicationMonitor.getInstance().addTimerMeasurement(name, startTime, endTime);
    }
  }
}
