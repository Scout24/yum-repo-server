package de.is24.infrastructure.gridfs.http.web.controller;

import de.is24.infrastructure.gridfs.http.web.boot.AbstractContainerAndMongoDBStarter;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static java.lang.Thread.sleep;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;


public class StatusControllerWithActiveDatabaseIT extends AbstractContainerAndMongoDBStarter {

  private static final Logger log = LoggerFactory.getLogger(StatusControllerWithActiveDatabaseIT.class);

  @Test
  public void statusPageIsPresent() throws Exception {
    int attempt = 1;
    while (attempt < 60) {
      try {
        checkStatus();
        break;
      } catch (AssertionError e) {
        log.warn("Attempt {} failed to check status.", attempt);
        attempt += 1;
        sleep(1000);
      }
    }
  }

  public void checkStatus() throws IOException {
    String statusUrl = deploymentURL + "/status";

    HttpGet get = new HttpGet(statusUrl);
    HttpResponse httpResponse = httpClient.execute(get);
    String jsonResponse = IOUtils.toString(httpResponse.getEntity().getContent());

    assertThat(httpResponse.getStatusLine().getStatusCode(), is(SC_OK));
    assertThat(jsonResponse, containsString("{mongoDBStatus: \"ok\""));
  }


}
