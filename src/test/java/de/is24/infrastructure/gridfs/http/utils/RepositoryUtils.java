package de.is24.infrastructure.gridfs.http.utils;

import static java.lang.System.currentTimeMillis;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static org.apache.http.auth.AuthScope.ANY_HOST;
import static org.apache.http.auth.AuthScope.ANY_PORT;
import static org.apache.http.entity.ContentType.APPLICATION_FORM_URLENCODED;
import static org.apache.http.util.EntityUtils.consume;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;


public final class RepositoryUtils {
  private RepositoryUtils() {
  }

  private static AtomicInteger counter = new AtomicInteger();

  public static String uniqueRepoName() {
    return uniqueRepoNameWithPrefix("repo");
  }

  public static String uniqueRepoNameWithPrefix(String prefix) {
    return prefix + "-" + currentTimeMillis() + "-" + counter.incrementAndGet();
  }

  public static String givenVirtualRepoLinkedToStatic(URL deploymentURL, String staticTargetRepoName)
                                               throws IOException {
    return givenVirtualRepo(deploymentURL, "static/" + staticTargetRepoName);
  }

  public static String givenVirtualRepo(URL deploymentURL, String destination) throws IOException {
    HttpPost post = new HttpPost(deploymentURL + "/repo/virtual");
    String newVirtualReponame = uniqueRepoName();
    post.setEntity(new StringEntity("name=" + newVirtualReponame + "&destination=" + destination,
        APPLICATION_FORM_URLENCODED));

    HttpResponse response = RepositoryUtils.newDefaultHttpClientWithCredentials().execute(post);
    consume(response.getEntity());

    assertThat(response.getStatusLine().getStatusCode(), is(SC_CREATED));
    return newVirtualReponame;
  }

  public static DefaultHttpClient newDefaultHttpClientWithCredentials() {
    DefaultHttpClient httpClient = new DefaultHttpClient();
    httpClient.getCredentialsProvider()
    .setCredentials(new AuthScope(ANY_HOST, ANY_PORT), new UsernamePasswordCredentials("anyuser", "anyuser"));
    return httpClient;
  }
}
