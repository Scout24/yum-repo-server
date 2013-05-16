package de.is24.infrastructure.gridfs.http.web.exception;

import de.is24.infrastructure.gridfs.http.exception.BadRequestException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletResponse;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

public class MessageAwareResponseStatusExceptionResolverTest {
  private MessageAwareResponseStatusExceptionResolver resolver;
  private HttpServletResponse httpServletResponse;
  private ResponseStatus responseStatus;

  @Before
  public void setup() {
    this.httpServletResponse = mock(HttpServletResponse.class);
    this.responseStatus = mock(ResponseStatus.class);
    this.resolver = new MessageAwareResponseStatusExceptionResolver();
  }

  @Test
  public void statuCodeAnnotatedOnExceptionIsRendered() throws Exception {
    when(responseStatus.value()).thenReturn(BAD_REQUEST);

    resolver.resolveResponseStatus(responseStatus, null, httpServletResponse, null, new BadRequestException("any"));

    verify(httpServletResponse).sendError(eq(SC_BAD_REQUEST), anyString());
  }

  @Test
  public void rendersExceptionMessageIfNoReasonIsGiven() throws Exception {
    String exceptionMessage = "Expected message text.";
    when(responseStatus.value()).thenReturn(INTERNAL_SERVER_ERROR);

    resolver.resolveResponseStatus(responseStatus, null, httpServletResponse, null, new BadRequestException(exceptionMessage));

    verify(httpServletResponse).sendError(anyInt(), eq(exceptionMessage));
  }

  @Test
  public void rendersReasonIfReasonIsGiven() throws Exception {
    String exceptionMessage = "any exception message";
    String reason = "Expected message text.";

    when(responseStatus.value()).thenReturn(BAD_REQUEST);
    when(responseStatus.reason()).thenReturn(reason);

    resolver.resolveResponseStatus(responseStatus, null, httpServletResponse, null, new BadRequestException(exceptionMessage));

    verify(httpServletResponse).sendError(anyInt(), eq(reason));
  }
}
