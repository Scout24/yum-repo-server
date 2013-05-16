package de.is24.infrastructure.gridfs.http.web.controller;

import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.uniqueRepoName;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.RPM_FILE;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.RPM_FILE_SIZE;
import static de.is24.infrastructure.gridfs.http.web.RepoTestUtils.uploadRpm;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_PARTIAL_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import de.is24.infrastructure.gridfs.http.web.AbstractContainerAndMongoDBStarter;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.junit.LocalOrRemoteDeploymentTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(LocalOrRemoteDeploymentTestRunner.class)
public class FileControllerIT extends AbstractContainerAndMongoDBStarter {
  private String repoUrl;

  @Before
  public void setUp() throws Exception {
    repoUrl = deploymentURL + "/repo/" + uniqueRepoName();

    uploadRpm(repoUrl, RPM_FILE.getPath());
  }

  @Test
  public void downloadedFileWithRange() throws Exception {
    HttpGet get = new HttpGet(repoUrl + "/noarch/test-artifact-1.2-1.noarch.rpm");
    get.addHeader("Range", "bytes=0-1023");

    HttpResponse response = httpClient.execute(get);
    assertThat(response.getEntity().getContentLength(), is(1024L));
    assertThat(response.getFirstHeader("Content-Type").getValue(), is("application/x-rpm"));
    assertThat(response.getFirstHeader("Content-Length").getValue(), is("1024"));
    assertThat(response.getStatusLine().getStatusCode(), is(SC_PARTIAL_CONTENT));
  }

  /**
   * @see http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.35
   * @see http://stackoverflow.com/questions/3303029/http-range-header
  */
  @Test
  public void downloadedFileWithRangeStartingInBetween() throws Exception {
    HttpGet get = new HttpGet(repoUrl + "/noarch/test-artifact-1.2-1.noarch.rpm");
    get.addHeader("Range", "bytes=500-1023");

    HttpResponse response = httpClient.execute(get);
    assertThat(response.getEntity().getContentLength(), is(524L));
    assertThat(response.getFirstHeader("Content-Type").getValue(), is("application/x-rpm"));
    assertThat(response.getFirstHeader("Content-Length").getValue(), is("524"));
    assertThat(response.getStatusLine().getStatusCode(), is(SC_PARTIAL_CONTENT));
  }

  /** @see #downloadedFileWithRangeStartingInBetween() */
  @Test
  public void returnBadRequestRangeForStartBiggerFileSize() throws Exception {
    HttpGet get = new HttpGet(repoUrl + "/noarch/test-artifact-1.2-1.noarch.rpm");
    get.addHeader("Range", "bytes=2000-2001");

    HttpResponse response = httpClient.execute(get);
    String body = EntityUtils.toString(response.getEntity());

    assertThat(response.getStatusLine().getStatusCode(), is(SC_REQUESTED_RANGE_NOT_SATISFIABLE));
    assertThat(body, containsString("/noarch/test-artifact-1.2-1.noarch.rpm"));
    assertThat(body, containsString("2000"));
    assertThat(body, containsString("1364"));
  }

  @Test
  public void returnBadRequestRangeForStartGreaterEnd() throws Exception {
    HttpGet get = new HttpGet(repoUrl + "/noarch/test-artifact-1.2-1.noarch.rpm");
    get.addHeader("Range", "bytes=2001-2000");

    HttpResponse response = httpClient.execute(get);
    String body = EntityUtils.toString(response.getEntity());

    assertThat(response.getStatusLine().getStatusCode(), is(SC_REQUESTED_RANGE_NOT_SATISFIABLE));
    assertThat(body, containsString("/noarch/test-artifact-1.2-1.noarch.rpm"));
    assertThat(body, containsString("2000"));
    assertThat(body, containsString("2001"));
  }

  @Test
  public void hasDownloadedWholeFile() throws Exception {
    HttpGet get = new HttpGet(repoUrl + "/noarch/test-artifact-1.2-1.noarch.rpm");

    HttpResponse response = httpClient.execute(get);
    assertThat(response.getEntity().getContentLength(), is((long) RPM_FILE_SIZE));
    assertThat(response.getFirstHeader("Content-Length").getValue(), is(Integer.toString(RPM_FILE_SIZE)));
    assertThat(response.getFirstHeader("Content-Type").getValue(), is("application/x-rpm"));
    assertThat(response.getStatusLine().getStatusCode(), is(SC_OK));
  }

}
