package de.is24.infrastructure.gridfs.http.utils;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static de.is24.infrastructure.gridfs.http.utils.HostnameResolver.X_FORWARDED_FOR;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.join;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class HostnameResolverTest {
  private static final String LOADBALANCER_IP = "10.99.10.12";
  private static final String LOADBALANCER_IP2 = "10.99.10.13";
  private static final String LOADBALANCER_IP3 = "10.99.10.14";
  private static final String UNTRUSTED_LOADBALANCER_IP = "10.99.10.99";
  private static final String ARBITRARY_IP = "192.168.5.5";
  private static final String ANOTHER_IP = "192.168.7.7";

  private HostnameResolver hostnameResolver;

  @Before
  public void setup() {
    hostnameResolver = new HostnameResolver(join(asList(LOADBALANCER_IP, LOADBALANCER_IP2, LOADBALANCER_IP3), ","));
  }

  @Test
  public void resolveHostnameFromIP() {
    MockHttpServletRequest request = request(ARBITRARY_IP);
    assertThat(hostnameResolver.remoteHost(request).getName(), is(ARBITRARY_IP));
  }

  @Test
  public void resolveHostnameFromForwardedForHeaderIfRequestFromTrustedLoadbalancer() {
    MockHttpServletRequest request = request(LOADBALANCER_IP);
    request.addHeader(X_FORWARDED_FOR, ARBITRARY_IP);
    assertThat(hostnameResolver.remoteHost(request).getName(), is(ARBITRARY_IP));
  }

  @Test
  public void ignoreForwardedForHeaderIfRequestFromUntrustedLoadbalancer() {
    MockHttpServletRequest request = request(UNTRUSTED_LOADBALANCER_IP);
    request.addHeader(X_FORWARDED_FOR, ARBITRARY_IP);
    assertThat(hostnameResolver.remoteHost(request).getName(), is(UNTRUSTED_LOADBALANCER_IP));
  }

  @Test
  public void takeLatestUntrustedXForwardedForHeaderElement() throws Exception {
    MockHttpServletRequest request = request(LOADBALANCER_IP);
    request.addHeader(X_FORWARDED_FOR, ANOTHER_IP + ", " + ARBITRARY_IP);
    assertThat(hostnameResolver.remoteHost(request).getName(), is(ARBITRARY_IP));
  }

  @Test
  public void allowMultipleTrustedLoadbalancers() throws Exception {
    MockHttpServletRequest request = request(LOADBALANCER_IP);
    request.addHeader(X_FORWARDED_FOR, ARBITRARY_IP + "," + LOADBALANCER_IP2 + "," + LOADBALANCER_IP3);
    assertThat(hostnameResolver.remoteHost(request).getName(), is(ARBITRARY_IP));
  }

  @Test
  public void allowMultipleTrustedLoadbalancersButFindTheUntrustedOne() throws Exception {
    MockHttpServletRequest request = request(LOADBALANCER_IP);
    request.addHeader(X_FORWARDED_FOR, ARBITRARY_IP + "," + LOADBALANCER_IP2 + "," + UNTRUSTED_LOADBALANCER_IP + "," + LOADBALANCER_IP3);
    assertThat(hostnameResolver.remoteHost(request).getName(), is(UNTRUSTED_LOADBALANCER_IP));
  }

  @Test
  public void allowMultipleTrustedLoadbalancersButFindLast() throws Exception {
    MockHttpServletRequest request = request(LOADBALANCER_IP);
    request.addHeader(X_FORWARDED_FOR, LOADBALANCER_IP2 + "," + LOADBALANCER_IP3);
    assertThat(hostnameResolver.remoteHost(request).getName(), is(LOADBALANCER_IP2));
  }

  private MockHttpServletRequest request(String ip) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteHost(ip);
    request.setRemoteAddr(ip);
    return request;
  }


}
