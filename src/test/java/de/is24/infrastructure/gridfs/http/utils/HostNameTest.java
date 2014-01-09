package de.is24.infrastructure.gridfs.http.utils;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;


public class HostNameTest {
  public static final String BLA_BLI_BLU = "bla.bli.blu";
  public static final String BLA = "bla";

  @Test
  public void ipIsDetected() {
    HostName hostName = new HostName("127.0.0.1");
    assertThat(hostName.isIp(), is(true));
  }

  @Test
  public void ipHasNoShortName() {
    HostName hostName = new HostName("127.0.0.1");
    assertThat(hostName.getShortName(), nullValue());
  }

  @Test
  public void fqdnIsNotIp() {
    HostName hostName = new HostName(BLA_BLI_BLU);
    assertThat(hostName.isIp(), is(false));
  }


  @Test
  public void fqdnStaysFqdn() {
    HostName hostName = new HostName(BLA_BLI_BLU);
    assertThat(hostName.getName(), is(BLA_BLI_BLU));
  }

  @Test
  public void determineShortNameFromFqdn() {
    HostName hostName = new HostName(BLA_BLI_BLU);
    assertThat(hostName.getShortName(), is(BLA));
  }

  @Test
  public void shortAsInputStaysShortInName() {
    HostName hostName = new HostName(BLA);
    assertThat(hostName.getName(), is(BLA));
  }

  @Test
  public void shortAsInputStaysShortInShortName() {
    HostName hostName = new HostName(BLA);
    assertThat(hostName.getShortName(), is(BLA));
  }

}
