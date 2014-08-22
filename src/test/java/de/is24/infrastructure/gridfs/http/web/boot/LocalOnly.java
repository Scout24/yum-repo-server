package de.is24.infrastructure.gridfs.http.web.boot;

import org.springframework.test.annotation.IfProfileValue;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static de.is24.infrastructure.gridfs.http.web.boot.LocalConfig.LOCAL_KEY;
import static de.is24.infrastructure.gridfs.http.web.boot.LocalConfig.TRUE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * the annotatoin signals the Spring that the test
 * method is only executed in local deployment mode
 */
@Retention(RUNTIME)
@Target({METHOD, TYPE})
@IfProfileValue(name = LOCAL_KEY, value=TRUE)
public @interface LocalOnly {
}
