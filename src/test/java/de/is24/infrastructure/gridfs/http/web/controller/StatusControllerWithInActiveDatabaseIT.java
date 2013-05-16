package de.is24.infrastructure.gridfs.http.web.controller;

import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import de.is24.infrastructure.gridfs.http.web.AbstractContainerAndMongoDBStarter;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.jboss.arquillian.junit.LocalOnly;
import org.jboss.arquillian.junit.LocalOrRemoteDeploymentTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(LocalOrRemoteDeploymentTestRunner.class)
public class StatusControllerWithInActiveDatabaseIT extends AbstractContainerAndMongoDBStarter {
  @LocalOnly
  @Test
  public void statusPageIsPresentWithInactiveDB() throws Throwable {
    stopMongo();

    String statusUrl = deploymentURL + "/status";

    HttpGet get = new HttpGet(statusUrl);
    HttpResponse httpResponse = httpClient.execute(get);
    String jsonResponse = IOUtils.toString(httpResponse.getEntity().getContent());

    assertThat(httpResponse.getStatusLine().getStatusCode(), is(SC_SERVICE_UNAVAILABLE));
    assertThat(jsonResponse, is(equalTo("{mongoDBStatus: 'not responding'}")));

    startMongo();
  }


}
