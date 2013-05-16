package de.is24.infrastructure.gridfs.http.utils.retry;

import de.is24.infrastructure.gridfs.http.utils.retry.RetryUtils.Retryable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import static de.is24.infrastructure.gridfs.http.utils.retry.RetryUtils.execute;


@Aspect
@Component
public class RetryAspect {
  @Around("@annotation(de.is24.infrastructure.gridfs.http.utils.retry.Retry)")
  public Object executeWithRetry(final ProceedingJoinPoint pjp) throws Throwable {
    MethodSignature signature = (MethodSignature) pjp.getSignature();
    Retry retryAnnotation = signature.getMethod().getAnnotation(Retry.class);

    return execute().maxTries(retryAnnotation.maxTries())
      .wait(retryAnnotation.secondsToWait())
      .command(new Retryable<Object>() {
          @Override
          public Object run() throws Throwable {
            return pjp.proceed();
          }

        });
  }

}
