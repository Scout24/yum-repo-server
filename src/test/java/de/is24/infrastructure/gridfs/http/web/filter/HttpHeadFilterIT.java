package de.is24.infrastructure.gridfs.http.web.filter;

import de.is24.infrastructure.gridfs.http.web.AbstractContainerAndMongoDBStarter;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.jboss.arquillian.junit.LocalOrRemoteDeploymentTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.givenVirtualRepoLinkedToStatic;
import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.uniqueRepoName;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.RPM_FILE;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.RPM_FILE_LOCATION;
import static de.is24.infrastructure.gridfs.http.web.RepoTestUtils.uploadRpm;
import static de.is24.infrastructure.gridfs.http.web.UrlUtils.join;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(LocalOrRemoteDeploymentTestRunner.class)
public class HttpHeadFilterIT extends AbstractContainerAndMongoDBStarter {

  private List<String> urlsToTest = new ArrayList<>();

  @Before
  public void setUp() throws Exception {
    String staticReponame = uniqueRepoName();
    String staticRepoListUrl = join(deploymentURL, "/repo/");
    String staticRepoUrl = staticRepoListUrl + staticReponame + "/";
    uploadRpm(staticRepoUrl, RPM_FILE.getPath());
    urlsToTest.add(staticRepoListUrl);
    urlsToTest.add(staticRepoUrl);
    urlsToTest.add(staticRepoUrl + RPM_FILE_LOCATION);

    String virtualReponame = givenVirtualRepoLinkedToStatic(deploymentURL, staticReponame);

    String virtualRepoListUrl = join(deploymentURL , "/repo/virtual/");
    String virtualRepoUrl = virtualRepoListUrl + virtualReponame + "/";
    urlsToTest.add(virtualRepoListUrl);
    urlsToTest.add(virtualRepoUrl);
    urlsToTest.add(virtualRepoUrl + RPM_FILE_LOCATION);
  }

  @Test
  public void shouldDeliverHeadRequests() throws Exception {
    for (String url : urlsToTest) {
      assertCorrectHeadRequest(url);
    }
  }

  private void assertCorrectHeadRequest(String url) throws IOException {
    HttpHead head = new HttpHead(url);
    CloseableHttpResponse response = httpClient.execute(head);
    assertThat(url + " has not responded with 200", response.getStatusLine().getStatusCode(), is(SC_OK));
    assertThat(url + " did send a response body", response.getEntity(), nullValue());
  }
}
