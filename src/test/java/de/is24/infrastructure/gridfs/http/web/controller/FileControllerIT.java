package de.is24.infrastructure.gridfs.http.web.controller;

import de.is24.infrastructure.gridfs.http.metadata.YumEntriesRepository;
import de.is24.infrastructure.gridfs.http.web.boot.AbstractContainerAndMongoDBStarter;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.uniqueRepoName;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.RPM_FILE;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.RPM_FILE_SIZE;
import static de.is24.infrastructure.gridfs.http.web.RepoTestUtils.uploadRpm;
import static java.lang.String.format;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_PARTIAL_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE;
import static org.apache.http.util.EntityUtils.consume;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;


public class FileControllerIT extends AbstractContainerAndMongoDBStarter {
  private static final int[] RHEL_VERSION_NUMBERS = {5, 6, 7};
  private String repoUrl;
  private String repoName;

  @Autowired
  YumEntriesRepository yumEntriesRepository;

  @Before
  public void setUp() throws Exception {
    repoName = uniqueRepoName();
    repoUrl = deploymentURL + "/repo/" + repoName;

    uploadRpm(repoUrl, RPM_FILE.getPath());
  }

  @Test
  public void downloadedFileWithRange() throws Exception {
    HttpGet get = new HttpGet(repoUrl + "/noarch/test-artifact-1.2-1.noarch.rpm");
    get.addHeader("Range", "bytes=0-1023");

    HttpResponse response = httpClient.execute(get);
    assertThat(response.getStatusLine().getStatusCode(), is(SC_PARTIAL_CONTENT));
    assertThat(response.getEntity().getContentLength(), is(1024L));
    assertThat(response.getFirstHeader("Content-Type").getValue(), is("application/x-rpm"));
    assertThat(response.getFirstHeader("Content-Length").getValue(), is("1024"));
  }

  @Test
  public void deleteRpmFileAndMetadata() throws Exception {
    HttpDelete delete = new HttpDelete(repoUrl + "/noarch/test-artifact-1.2-1.noarch.rpm");
    HttpResponse response = httpClient.execute(delete);
    assertThat(response.getStatusLine().getStatusCode(), is(SC_NO_CONTENT));
    consume(response.getEntity());

    HttpGet get = new HttpGet(repoUrl + "/noarch/test-artifact-1.2-1.noarch.rpm");
    response = httpClient.execute(get);
    assertThat(response.getStatusLine().getStatusCode(), is(SC_NOT_FOUND));

    assertThat(yumEntriesRepository.findByRepoAndYumPackageName(repoName, "test-artifact"), empty());
  }

  /**
   * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.35">RFC-2616</a>
   * <a href="http://stackoverflow.com/questions/3303029/http-range-header">HTTP Range Header</a>
  */
  @Test
  public void downloadedFileWithRangeStartingInBetween() throws Exception {
    HttpGet get = new HttpGet(repoUrl + "/noarch/test-artifact-1.2-1.noarch.rpm");
    get.addHeader("Range", "bytes=500-1023");

    HttpResponse response = httpClient.execute(get);
    assertThat(response.getStatusLine().getStatusCode(), is(SC_PARTIAL_CONTENT));
    assertThat(response.getEntity().getContentLength(), is(524L));
    assertThat(response.getFirstHeader("Content-Type").getValue(), is("application/x-rpm"));
    assertThat(response.getFirstHeader("Content-Length").getValue(), is("524"));
  }

  /** @see #downloadedFileWithRangeStartingInBetween() */
  @Test
  public void returnBadRequestRangeForStartBiggerFileSize() throws Exception {
    HttpGet get = new HttpGet(repoUrl + "/noarch/test-artifact-1.2-1.noarch.rpm");
    get.addHeader("Range", "bytes=2000-2001");

    HttpResponse response = httpClient.execute(get);
    consume(response.getEntity());

    assertThat(response.getStatusLine().getStatusCode(), is(SC_REQUESTED_RANGE_NOT_SATISFIABLE));
  }

  @Test
  public void returnBadRequestRangeForStartGreaterEnd() throws Exception {
    HttpGet get = new HttpGet(repoUrl + "/noarch/test-artifact-1.2-1.noarch.rpm");
    get.addHeader("Range", "bytes=2001-2000");

    HttpResponse response = httpClient.execute(get);
    consume(response.getEntity());

    assertThat(response.getStatusLine().getStatusCode(), is(SC_REQUESTED_RANGE_NOT_SATISFIABLE));
  }

  @Test
  public void hasDownloadedWholeFile() throws Exception {
    checkRpmDownload(repoUrl);
  }

  @Test
  public void downloadFileWithRewriteRule() throws Exception {
    String repoPrefix = deploymentURL + "/repo/" + uniqueRepoName();
    for (int rhelVersionNumber : RHEL_VERSION_NUMBERS) {
      uploadRpm(format("%s-rhel-%dX-test", repoPrefix, rhelVersionNumber), RPM_FILE.getPath());
      checkRpmDownload(format("%s-rhel-%d-test", repoPrefix, rhelVersionNumber));
      checkRpmDownload(format("%s-rhel-%d.5-test", repoPrefix, rhelVersionNumber));
    }
  }

  public void checkRpmDownload(String repo) throws IOException {
    HttpGet get = new HttpGet(repo + "/noarch/test-artifact-1.2-1.noarch.rpm");
    HttpResponse response = httpClient.execute(get);
    assertThat(response.getStatusLine().getStatusCode(), is(SC_OK));
    assertThat(response.getEntity().getContentLength(), is((long) RPM_FILE_SIZE));
    assertThat(response.getFirstHeader("Content-Length").getValue(), is(Integer.toString(RPM_FILE_SIZE)));
    assertThat(response.getFirstHeader("Content-Type").getValue(), is("application/x-rpm"));
    consume(response.getEntity());
  }
}
