package de.is24.infrastructure.gridfs.http.web;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.assertEquals;


public class TrailingSlashRedirectHandlerInterceptorTest {

  @Test
  public void shouldNotRedirect() throws Exception {
    final TrailingSlashRedirectHandlerInterceptor handlerInterceptor = new TrailingSlashRedirectHandlerInterceptor();

    final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
    final MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

    final boolean preHandle = handlerInterceptor.preHandle(mockHttpServletRequest, mockHttpServletResponse, null);

    assertEquals(true, preHandle);
  }

  @Test
  public void shouldRedirectToSlashForRepoRequest() throws Exception {
    final TrailingSlashRedirectHandlerInterceptor handlerInterceptor = new TrailingSlashRedirectHandlerInterceptor();

    final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
    final MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

    mockHttpServletRequest.setRequestURI("/repo/myrepo");
    mockHttpServletRequest.setMethod("GET");

    final boolean preHandle = handlerInterceptor.preHandle(mockHttpServletRequest, mockHttpServletResponse, null);

    assertEquals(false, preHandle);
  }

  @Test
  public void shouldRedirectToSlashForVirtualRequest() throws Exception {
    final TrailingSlashRedirectHandlerInterceptor handlerInterceptor = new TrailingSlashRedirectHandlerInterceptor();

    final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
    final MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

    mockHttpServletRequest.setRequestURI("/repo/virtual/myrepo");
    mockHttpServletRequest.setMethod("GET");

    final boolean preHandle = handlerInterceptor.preHandle(mockHttpServletRequest, mockHttpServletResponse, null);

    assertEquals(false, preHandle);
  }

  @Test
  public void shouldNotRedirectHtmlRequest() throws Exception {
    final TrailingSlashRedirectHandlerInterceptor handlerInterceptor = new TrailingSlashRedirectHandlerInterceptor();

    final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
    final MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

    mockHttpServletRequest.setRequestURI("/repo/info.html");
    mockHttpServletRequest.setMethod("GET");

    final boolean preHandle = handlerInterceptor.preHandle(mockHttpServletRequest, mockHttpServletResponse, null);

    assertEquals(true, preHandle);
  }

  @Test
  public void shouldNotRedirectJsonRequest() throws Exception {
    final TrailingSlashRedirectHandlerInterceptor handlerInterceptor = new TrailingSlashRedirectHandlerInterceptor();

    final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
    final MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

    mockHttpServletRequest.setRequestURI("/repo/info.json");
    mockHttpServletRequest.setMethod("GET");

    final boolean preHandle = handlerInterceptor.preHandle(mockHttpServletRequest, mockHttpServletResponse, null);

    assertEquals(true, preHandle);
  }
}