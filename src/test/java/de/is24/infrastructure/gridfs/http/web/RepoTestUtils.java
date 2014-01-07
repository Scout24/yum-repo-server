package de.is24.infrastructure.gridfs.http.web;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import java.io.File;
import java.io.IOException;

import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.getHttpClientBuilder;
import static org.apache.http.entity.ContentType.MULTIPART_FORM_DATA;
import static org.apache.http.entity.mime.HttpMultipartMode.BROWSER_COMPATIBLE;
import static org.apache.http.util.EntityUtils.consume;


public final class RepoTestUtils {
  private RepoTestUtils() {
  }

  public static HttpResponse uploadRpm(String repoUrl, String pathToRpm) throws IOException {
    CloseableHttpClient httpClient = getDefaultHttpClient();

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

  public static void addTagToRepo(String repoUrl, String tagName) throws IOException {
    CloseableHttpClient httpClient = getDefaultHttpClient();

    String postUrl = repoUrl + "/tags/";
    HttpPost post = new HttpPost(postUrl);
    post.setEntity(new StringEntity("tag=" + tagName, (ContentType) null));

    HttpResponse response = httpClient.execute(post);
    consume(response.getEntity());
  }

  private static CloseableHttpClient getDefaultHttpClient() {
    return getHttpClientBuilder("user", "user").build();
  }

  public static HttpResponse createRepository(String repositoryUrl, String repositoryName) throws IOException {
    String postUrl = repositoryUrl + "/repo";

    HttpPost post = new HttpPost(postUrl);
    post.setEntity(new StringEntity("name=" + repositoryName, (ContentType) null));
    return getDefaultHttpClient().execute(post);
  }
}
