package de.is24.infrastructure.gridfs.http.web.filter;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class FormEncodedContentTypeFilterTest {

  public static final String CONTENT_TYPE = "Content-Type";
  public static final String ANY_TYPE = "any-type";
  private FormEncodedContentTypeFilter filter;
  private FilterChain filterChain;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @Before
  public void setUp() throws Exception {
    filter = new FormEncodedContentTypeFilter();
    filterChain = mock(FilterChain.class);
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
  }

  @Test
  public void notWrapGetRequests() throws Exception {
    request.setMethod("GET");
    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(eq(request), eq(response));
  }

  @Test
  public void notWrapPostRequestsWithContentType() throws Exception {
    request.setMethod("POST");
    request.addHeader(CONTENT_TYPE, ANY_TYPE);
    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(eq(request), eq(response));
  }

  @Test
  public void wrapPostRequestsWithoutContentType() throws Exception {
    request.setMethod("POST");
    request.setContent(new byte[0]);
    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(any(FormEncodedHttpServletRequestWrapper.class), eq(response));
  }
}
