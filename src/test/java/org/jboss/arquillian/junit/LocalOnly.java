package org.jboss.arquillian.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * the annotatoin signals the {@link LocalOrRemoteDeploymentTestRunner} that the test
 * method is only executed in local deployment mode
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface LocalOnly {
}
