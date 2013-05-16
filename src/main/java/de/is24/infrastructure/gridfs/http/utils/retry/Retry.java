package de.is24.infrastructure.gridfs.http.utils.retry;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.METHOD;


/**
 * This underlying {@link RetryAspect} relies on
 * <a href="http://static.springsource.org/spring/docs/3.2.x/spring-framework-reference/html/aop.html">
 * Spring AOP</a>.
 *
 * Hence, it has the following <b>limitations</b>:
 * <ul>
 * <li>It does NOT support static methods.</li>
 * <li>It can ONLY be applied to spring beans.</li>
 * </ul>
 *
 * Side note: If this has changed or is just wrong (the original author is new to Spring AOP), please update the doc!
 *
 * @author mgruhn
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ METHOD })
public @interface Retry {
  int maxTries() default 2;

  int secondsToWait() default 1;
}
