package de.is24.infrastructure.gridfs.http.web;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import java.io.File;
import java.io.IOException;
import static org.apache.http.auth.AuthScope.ANY_HOST;
import static org.apache.http.auth.AuthScope.ANY_PORT;
import static org.apache.http.entity.ContentType.MULTIPART_FORM_DATA;
import static org.apache.http.entity.mime.HttpMultipartMode.BROWSER_COMPATIBLE;
import static org.apache.http.util.EntityUtils.consume;


public final class RepoTestUtils {
  private RepoTestUtils() {
  }

  public static HttpResponse uploadRpm(String repoUrl, String pathToRpm) throws IOException {
    CloseableHttpClient httpClient = getHttpClient();

    HttpPost post = new HttpPost(repoUrl);
    File rpmFile = new File(pathToRpm);
    HttpEntity entity = MultipartEntityBuilder.create().setMode(BROWSER_COMPATIBLE).addBinaryBody("rpmFile",
        rpmFile,
        MULTIPART_FORM_DATA, rpmFile.getName()).build();
    post.setEntity(entity);

    HttpResponse response = httpClient.execute(post);
    final int statusCode = response.getStatusLine().getStatusCode();
    if (statusCode > 299) {
      throw new RuntimeException("could not upload " + pathToRpm + " to " + repoUrl + "\nResponseCode:" + statusCode +
        "\nResponsebody: " + EntityUtils.toString(response.getEntity()));
    }

    consume(response.getEntity());
    return response;
  }

  private static CloseableHttpClient getHttpClient() {
    BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(new AuthScope(ANY_HOST, ANY_PORT), new UsernamePasswordCredentials("user", "user"));

    return HttpClientBuilder.create().setDefaultCredentialsProvider(credentialsProvider).build();
  }

  public static void addTagToRepo(String repoUrl, String tagName) throws IOException {
    CloseableHttpClient httpClient = getHttpClient();

    String postUrl = repoUrl + "/tags/";
    HttpPost post = new HttpPost(postUrl);
    post.setEntity(new StringEntity("tag=" + tagName, (ContentType) null));

    HttpResponse response = httpClient.execute(post);
    consume(response.getEntity());
  }

  public static HttpResponse createRepository(String repositoryUrl, String repositoryName) throws IOException {
    String postUrl = repositoryUrl + "/repo";

    HttpPost post = new HttpPost(postUrl);
    post.setEntity(new StringEntity("name=" + repositoryName, (ContentType) null));
    return getHttpClient().execute(post);
  }
}
