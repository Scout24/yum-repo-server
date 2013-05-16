package de.is24.infrastructure.gridfs.http.web;

import de.is24.infrastructure.gridfs.http.web.exception.MessageAwareResponseStatusExceptionResolver;
import org.junit.Test;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;


public class WebConfigTest {

  @Test
  public void smartResponseStatusExceptionResolverIsAvailable() {
    List<HandlerExceptionResolver> resolvers = new ArrayList<>();
    new WebConfig().configureHandlerExceptionResolvers(resolvers);
    assertThat(resolvers.get(0), instanceOf(MessageAwareResponseStatusExceptionResolver.class));
  }
}
