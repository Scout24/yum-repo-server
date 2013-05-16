package de.is24.infrastructure.gridfs.http.web.controller;

import static de.is24.infrastructure.gridfs.http.domain.RepoType.VIRTUAL;
import static de.is24.infrastructure.gridfs.http.mongo.IntegrationTestContext.mongoTemplate;
import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.givenVirtualRepo;
import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.givenVirtualRepoLinkedToStatic;
import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.uniqueRepoName;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.RPM_FILE;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.RPM_FILE_LOCATION;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.RPM_FILE_SIZE;
import static de.is24.infrastructure.gridfs.http.web.RepoTestUtils.uploadRpm;
import static javax.servlet.http.HttpServletResponse.SC_MOVED_TEMPORARILY;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.http.client.params.HttpClientParams.setRedirecting;
import static org.apache.http.util.EntityUtils.consume;
import static org.apache.http.util.EntityUtils.toByteArray;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import de.is24.infrastructure.gridfs.http.web.AbstractContainerAndMongoDBStarter;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.jboss.arquillian.junit.LocalOrRemoteDeploymentTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import de.is24.infrastructure.gridfs.http.domain.RepoEntry;
import de.is24.infrastructure.gridfs.http.metadata.RepoEntriesRepository;


@RunWith(LocalOrRemoteDeploymentTestRunner.class)
public class VirtualRepositoryControllerIT extends AbstractContainerAndMongoDBStarter {
  public static final String DESTINATION_DOMAIN = "http://any.domain/repo";
  private String repoUrl;
  private String reponame;
  private RepoEntriesRepository repository;
  private String virtualReponame;

  @Before
  public void setUp() throws Exception {
    reponame = uniqueRepoName();
    repoUrl = deploymentURL + "/repo/" + reponame;
    repository = new MongoRepositoryFactory(mongoTemplate(mongo)).getRepository(
      RepoEntriesRepository.class);

    uploadRpm(repoUrl, RPM_FILE.getPath());

    virtualReponame = givenVirtualRepoLinkedToStatic(deploymentURL, reponame);
  }

  @Test
  public void createVirtualRepo() throws Exception {
    RepoEntry repoEntry = repository.findFirstByName(virtualReponame);
    assertThat(repoEntry, notNullValue());
    assertThat(repoEntry.getType(), is(VIRTUAL));
  }

  @Test
  public void deleteVirtualRepo() throws Exception {
    HttpDelete delete = new HttpDelete(deploymentURL + "/repo/virtual/" + virtualReponame);

    HttpResponse response = httpClient.execute(delete);
    consume(response.getEntity());

    assertThat(response.getStatusLine().getStatusCode(), is(SC_NO_CONTENT));
    assertThat(repository.findFirstByName(virtualReponame), nullValue());
  }

  @Test
  public void downloadFileFromVirtualRepo() throws Exception {
    HttpGet get = new HttpGet(deploymentURL + "/repo/virtual/" + virtualReponame + "/" + RPM_FILE_LOCATION);
    HttpResponse response = httpClient.execute(get);

    byte[] content = toByteArray(response.getEntity());
    assertThat(response.getStatusLine().getStatusCode(), is(SC_OK));
    assertThat(content.length, is(RPM_FILE_SIZE));
  }

  @Test
  public void redirectToExternalRepo() throws Exception {
    virtualReponame = givenVirtualRepo(deploymentURL, DESTINATION_DOMAIN);
    HttpGet get = new HttpGet(deploymentURL + "/repo/virtual/" + virtualReponame + "/" + RPM_FILE_LOCATION);
    setRedirecting(httpClient.getParams(), false);
    HttpResponse response = httpClient.execute(get);
    consume(response.getEntity());

    assertThat(response.getStatusLine().getStatusCode(), is(SC_MOVED_TEMPORARILY));
    assertThat(response.getFirstHeader("Location").getValue(), is(DESTINATION_DOMAIN + "/" + RPM_FILE_LOCATION));
  }
}
