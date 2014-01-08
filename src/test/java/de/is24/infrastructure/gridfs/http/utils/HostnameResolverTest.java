package de.is24.infrastructure.gridfs.http.utils;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import javax.servlet.http.HttpServletRequest;


public class HostnameResolverTest {
  private static final String LOADBALANCER_IP = "10.99.10.12";
  private static final String ARBITRARY_IP = "192.168.5.5";
  private static final String ANOTHER_IP = "192.168.6.6";


  @Test
  public void resolveHostnameFromIP() {
    MockHttpServletRequest request = request(ARBITRARY_IP);

  }

  private HttpServletRequest proxiedRequest(String ip) {
    MockHttpServletRequest request = request(LOADBALANCER_IP);
    request.addHeader(HostnameResolver.X_FORWARDED_FOR, ip);
    return request;
  }

  private MockHttpServletRequest request(String ip) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteHost(ip);
    request.setRemoteAddr(ip);
    return request;
  }


}
