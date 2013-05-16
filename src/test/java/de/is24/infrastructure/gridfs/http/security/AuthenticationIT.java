package de.is24.infrastructure.gridfs.http.security;


import de.is24.infrastructure.gridfs.http.web.AbstractContainerAndMongoDBStarter;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.junit.LocalOnly;
import org.jboss.arquillian.junit.LocalOrRemoteDeploymentTestRunner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.apache.http.auth.AuthScope.ANY_HOST;
import static org.apache.http.auth.AuthScope.ANY_PORT;
import static org.apache.http.client.params.ClientPNames.VIRTUAL_HOST;
import static org.apache.http.util.EntityUtils.consume;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.is;

@RunWith(LocalOrRemoteDeploymentTestRunner.class)
public class AuthenticationIT extends AbstractContainerAndMongoDBStarter {

  private String deleteUrl;

  @Before
  public void setUp() throws Exception {
    deleteUrl = deploymentURL + "/repo/dev-repo/noarch/file.rpm";
  }

  @Test
  @LocalOnly
  public void denyAccessForNotWhiteListedHost() throws Exception {
    final DefaultHttpClient httpClientWithoutCredentials = new DefaultHttpClient();
    HttpDelete get = new HttpDelete(deleteUrl);
    HttpResponse response = httpClientWithoutCredentials.execute(get);
    consume(response.getEntity());

    assertThat(response.getStatusLine().getStatusCode(), is(SC_UNAUTHORIZED));
  }

  @Test
  @LocalOnly
  public void denyAccessForWrongCredentials() throws Exception {
    givenCredentials("user", "pass");
    HttpDelete get = new HttpDelete(deleteUrl);
    HttpResponse response = httpClient.execute(get);
    consume(response.getEntity());

    assertThat(response.getStatusLine().getStatusCode(), is(SC_UNAUTHORIZED));
  }

  @Test
  @LocalOnly
  public void allowAccessForForCorrectCredentials() throws Exception {
    givenCredentials("user", "user");
    HttpDelete get = new HttpDelete(deleteUrl);
    HttpResponse response = httpClient.execute(get);
    consume(response.getEntity());

    assertThat(response.getStatusLine().getStatusCode(), is(SC_NOT_FOUND));
  }

  @Test
  @Ignore
  @LocalOnly
  public void allowAccessForWhiteListedHost() throws Exception {
    HttpDelete get = new HttpDelete(deleteUrl);
    get.getParams().setParameter(VIRTUAL_HOST, new HttpHost("localhost"));
    HttpResponse response = httpClient.execute(get);
    consume(response.getEntity());

    assertThat(response.getStatusLine().getStatusCode(), is(SC_NOT_FOUND));
  }

  private void givenCredentials(String user, String password) {
    httpClient.getCredentialsProvider().setCredentials(new AuthScope(ANY_HOST, ANY_PORT), new UsernamePasswordCredentials(user, password));
  }
}
