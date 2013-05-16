package de.is24.infrastructure.gridfs.http.web;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import java.io.File;
import java.io.IOException;
import static org.apache.http.auth.AuthScope.ANY_HOST;
import static org.apache.http.auth.AuthScope.ANY_PORT;
import static org.apache.http.entity.mime.HttpMultipartMode.BROWSER_COMPATIBLE;
import static org.apache.http.util.EntityUtils.consume;


public final class RepoTestUtils {
  private RepoTestUtils() {
  }

  public static HttpResponse uploadRpm(String repoUrl, String pathToRpm) throws IOException {
    DefaultHttpClient httpClient = getHttpClient();

    HttpPost post = new HttpPost(repoUrl);
    MultipartEntity entity = new MultipartEntity(BROWSER_COMPATIBLE);
    entity.addPart("rpmFile",
      new FileBody((new File(pathToRpm)),
        "multipart/form-data"));
    post.setEntity(entity);

    HttpResponse response = httpClient.execute(post);
    consume(response.getEntity());
    return response;
  }

  private static DefaultHttpClient getHttpClient() {
    DefaultHttpClient httpClient = new DefaultHttpClient();
    httpClient.getCredentialsProvider()
    .setCredentials(new AuthScope(ANY_HOST, ANY_PORT), new UsernamePasswordCredentials("user", "user"));
    return httpClient;
  }

  public static void addTagToRepo(String repoUrl, String tagName) throws IOException {
    DefaultHttpClient httpClient = getHttpClient();

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
