package de.is24.infrastructure.gridfs.http.web.controller;

import de.is24.infrastructure.gridfs.http.web.boot.AbstractContainerAndMongoDBStarter;
import de.is24.infrastructure.gridfs.http.web.boot.LocalOnly;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.junit.Ignore;
import org.junit.Test;

import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

//TODO:
@Ignore("Atm it doesn't work with Spring Boot because the application is not reloaded")
@LocalOnly
public class StatusControllerWithInactiveDatabaseIT extends AbstractContainerAndMongoDBStarter {

  @Test
  public void statusPageIsPresentWithInactiveDB() throws Throwable {
    stopMongo();

    String statusUrl = deploymentURL + "/status";

    HttpGet get = new HttpGet(statusUrl);
    HttpResponse httpResponse = httpClient.execute(get);
    String jsonResponse = IOUtils.toString(httpResponse.getEntity().getContent());

    assertThat(httpResponse.getStatusLine().getStatusCode(), is(SC_SERVICE_UNAVAILABLE));
    assertThat(jsonResponse, containsString("{mongoDBStatus: \"not responding\"}"));

    startMongo();
  }


}
