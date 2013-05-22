package de.is24.infrastructure.gridfs.http.web.controller;

import de.is24.infrastructure.gridfs.http.web.AbstractContainerAndMongoDBStarter;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.jboss.arquillian.junit.LocalOnly;
import org.jboss.arquillian.junit.LocalOrRemoteDeploymentTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;


@RunWith(LocalOrRemoteDeploymentTestRunner.class)
public class StatusControllerWithInactiveDatabaseIT extends AbstractContainerAndMongoDBStarter {
  private static final Logger logger = LoggerFactory.getLogger(StatusControllerWithInactiveDatabaseIT.class);

  @LocalOnly
  @Test
  public void statusPageIsPresentWithInactiveDB() throws Throwable {
    stopMongo();

    String statusUrl = deploymentURL + "/status";

    logger.error("status: " + statusUrl);

    HttpGet get = new HttpGet(statusUrl);
    HttpResponse httpResponse = httpClient.execute(get);
    String jsonResponse = IOUtils.toString(httpResponse.getEntity().getContent());
    logger.error(jsonResponse);

    assertThat(httpResponse.getStatusLine().getStatusCode(), is(SC_SERVICE_UNAVAILABLE));
    assertThat(jsonResponse, startsWith("{mongoDBStatus: 'not responding'}"));

    startMongo();
  }


}
