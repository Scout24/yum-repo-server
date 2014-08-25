package de.is24.infrastructure.gridfs.http.security;


import de.is24.infrastructure.gridfs.http.web.boot.AbstractContainerAndMongoDBStarter;
import de.is24.infrastructure.gridfs.http.web.boot.LocalOnly;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URL;

import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.getHttpClientBuilder;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.apache.http.util.EntityUtils.consume;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;


@LocalOnly
public class AuthenticationIT extends AbstractContainerAndMongoDBStarter {
  private String deleteUrl;

  @Autowired
  WhiteListAuthenticationFilter whiteListAuthenticationFilter;

  @Before
  public void setUp() throws Exception {
    deleteUrl = deploymentURL + "/repo/dev-repo/noarch/file.rpm";
    whiteListAuthenticationFilter.setWhiteListedHosts("");
  }

  @Test
  public void denyAccessForNotWhiteListedHost() throws Exception {
    final CloseableHttpClient httpClientWithoutCredentials = HttpClientBuilder.create().build();
    HttpDelete get = new HttpDelete(deleteUrl);
    HttpResponse response = httpClientWithoutCredentials.execute(get);
    consume(response.getEntity());

    assertThat(response.getStatusLine().getStatusCode(), is(SC_UNAUTHORIZED));
  }

  @Test
  public void denyAccessForWrongCredentials() throws Exception {
    givenCredentials("user", "pass");

    HttpDelete get = new HttpDelete(deleteUrl);
    HttpResponse response = httpClient.execute(get);
    consume(response.getEntity());

    assertThat(response.getStatusLine().getStatusCode(), is(SC_UNAUTHORIZED));
  }

  @Test
  public void allowAccessForForCorrectCredentials() throws Exception {
    givenCredentials("user", "user");

    HttpDelete get = new HttpDelete(deleteUrl);
    HttpResponse response = httpClient.execute(get);
    consume(response.getEntity());

    assertThat(response.getStatusLine().getStatusCode(), is(SC_NO_CONTENT));
  }

  @Test
  public void allowAccessForWhiteListedHost() throws Exception {
    whiteListAuthenticationFilter.setWhiteListedHosts("localhost");
    givenCredentials("user", "wrong-password");

    URL url = new URL(deleteUrl);
    HttpHost httpHost = new HttpHost(url.getHost(), url.getPort());
    String newDeleteUrl = deleteUrl.replace("://" + url.getHost(), "://localhost");

    HttpDelete get = new HttpDelete(newDeleteUrl);
    HttpResponse response = httpClient.execute(httpHost, get);
    consume(response.getEntity());

    assertThat(response.getStatusLine().getStatusCode(), is(SC_NO_CONTENT));
  }

  @Test
  public void sendAuthenticateHeader() throws Exception {
    givenCredentials("user", "pass");

    HttpDelete delete = new HttpDelete(deleteUrl);
    delete.addHeader("X-Requested-With", "XMLHttpRequest");

    HttpResponse response = httpClient.execute(delete);
    consume(response.getEntity());

    assertThat(response.getStatusLine().getStatusCode(), is(SC_UNAUTHORIZED));
    Header authHeader = response.getFirstHeader("WWW-Authenticate");
    assertThat(authHeader, notNullValue());
    assertThat(authHeader.getValue(), containsString("Basic"));
  }

  private void givenCredentials(String user, String password) {
    httpClient = getHttpClientBuilder(user, password).build();
  }
}
