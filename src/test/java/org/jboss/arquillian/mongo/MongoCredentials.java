package org.jboss.arquillian.mongo;

import java.lang.annotation.*;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Inherited
@Documented
@Retention(RUNTIME)
@Target({ElementType.FIELD})
public @interface MongoCredentials {

  String username();

  String password();

  String db();
}
