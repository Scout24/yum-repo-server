package de.is24.infrastructure.gridfs.http.security;


import de.is24.infrastructure.gridfs.http.web.AbstractContainerAndMongoDBStarter;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.junit.LocalOnly;
import org.jboss.arquillian.junit.LocalOrRemoteDeploymentTestRunner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.getHttpClientBuilder;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.apache.http.client.params.ClientPNames.VIRTUAL_HOST;
import static org.apache.http.util.EntityUtils.consume;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


@RunWith(LocalOrRemoteDeploymentTestRunner.class)
public class AuthenticationIT extends AbstractContainerAndMongoDBStarter {
  private String deleteUrl;

  @Before
  public void setUp() throws Exception {
    deleteUrl = deploymentURL + "/repo/dev-repo/noarch/file.rpm";
  }

  @LocalOnly
  @Test
  public void denyAccessForNotWhiteListedHost() throws Exception {
    final CloseableHttpClient httpClientWithoutCredentials = HttpClientBuilder.create().build();
    HttpDelete get = new HttpDelete(deleteUrl);
    HttpResponse response = httpClientWithoutCredentials.execute(get);
    consume(response.getEntity());

    assertThat(response.getStatusLine().getStatusCode(), is(SC_UNAUTHORIZED));
  }

  @LocalOnly
  @Test
  public void denyAccessForWrongCredentials() throws Exception {
    givenCredentials("user", "pass");

    HttpDelete get = new HttpDelete(deleteUrl);
    HttpResponse response = httpClient.execute(get);
    consume(response.getEntity());

    assertThat(response.getStatusLine().getStatusCode(), is(SC_UNAUTHORIZED));
  }

  @LocalOnly
  @Test
  public void allowAccessForForCorrectCredentials() throws Exception {
    givenCredentials("user", "user");

    HttpDelete get = new HttpDelete(deleteUrl);
    HttpResponse response = httpClient.execute(get);
    consume(response.getEntity());

    assertThat(response.getStatusLine().getStatusCode(), is(SC_NO_CONTENT));
  }

  @Ignore
  @LocalOnly
  @Test
  public void allowAccessForWhiteListedHost() throws Exception {
    URL url = new URL(deleteUrl);
    HttpHost httpHost = new HttpHost(url.getHost(), url.getPort());
    String newDeleteUrl = deleteUrl.replace("://" + url.getHost(), "://localhost");

    HttpDelete get = new HttpDelete(newDeleteUrl);
    HttpResponse response = httpClient.execute(httpHost, get);
    consume(response.getEntity());

    assertThat(response.getStatusLine().getStatusCode(), is(SC_NOT_FOUND));
  }

  private void givenCredentials(String user, String password) {
    httpClient = getHttpClientBuilder(user, password).build();
  }
}
