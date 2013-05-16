package de.is24.infrastructure.gridfs.http.web.filter;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;

import static java.util.Collections.list;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class FormEncodedHttpServletRequestWrapperTest {

  private FormEncodedHttpServletRequestWrapper wrapper;

  @Test
  public void wrapEmptyRequests() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContent(new byte[0]);
    wrapper = wrap(request);

    assertThat(wrapper.getParameter("any-header"), nullValue());
  }

  @Test
  public void parseParameters() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContent("param1=value1&param2=value2&param1=value3".getBytes());
    wrapper = wrap(request);

    assertThat(wrapper.getParameter("param1"), is("value1"));
    assertThat(wrapper.getParameter("param2"), is("value2"));
    assertThat(list(wrapper.getParameterNames()), hasItems("param1", "param2"));
    assertThat(wrapper.getParameterValues("param1"), is(new String[] {"value1", "value3"}));
  }

  private FormEncodedHttpServletRequestWrapper wrap(MockHttpServletRequest request) {
    return new FormEncodedHttpServletRequestWrapper(request, new FormHttpMessageConverter());
  }
}
