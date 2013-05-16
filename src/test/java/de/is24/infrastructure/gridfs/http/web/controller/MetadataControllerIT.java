package de.is24.infrastructure.gridfs.http.web.controller;

import de.is24.infrastructure.gridfs.http.web.AbstractContainerAndMongoDBStarter;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.jboss.arquillian.junit.LocalOrRemoteDeploymentTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.uniqueRepoName;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.RPM_FILE;
import static de.is24.infrastructure.gridfs.http.web.RepoTestUtils.uploadRpm;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.http.util.EntityUtils.consume;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(LocalOrRemoteDeploymentTestRunner.class)
public class MetadataControllerIT extends AbstractContainerAndMongoDBStarter {

  private String repoUrl;
  private String reponame;

  @Before
  public void setUp() throws Exception {
    reponame = uniqueRepoName();
    repoUrl = deploymentURL + "/repo/" + reponame;
    uploadRpm(repoUrl,  RPM_FILE.getPath());
  }

  @Test
  public void generateMetadata() throws Exception {
    HttpPost post = new HttpPost(repoUrl + "/repodata");
    HttpResponse response = httpClient.execute(post);
    consume(response.getEntity());

    assertThat(response.getStatusLine().getStatusCode(), is(SC_CREATED));
    downloadFile("repomd.xml");
  }

  private void downloadFile(String filename) throws Exception {
    HttpGet get = new HttpGet(repoUrl + "/repodata/" + filename);
    HttpResponse response = httpClient.execute(get);
    consume(response.getEntity());
    assertThat(response.getStatusLine().getStatusCode(), is(SC_OK));
  }
}
