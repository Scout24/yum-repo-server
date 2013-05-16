package de.is24.infrastructure.gridfs.http.web.controller;

import de.is24.infrastructure.gridfs.http.domain.RepoEntry;
import de.is24.infrastructure.gridfs.http.metadata.RepoEntriesRepository;
import de.is24.infrastructure.gridfs.http.web.AbstractContainerAndMongoDBStarter;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.fest.assertions.api.Assertions;
import org.hamcrest.Matcher;
import org.jboss.arquillian.junit.LocalOrRemoteDeploymentTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import java.io.IOException;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.function.matcher.HasArgumentWithValue.havingValue;
import static de.is24.infrastructure.gridfs.http.domain.RepoType.SCHEDULED;
import static de.is24.infrastructure.gridfs.http.mongo.IntegrationTestContext.mongoTemplate;
import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.uniqueRepoName;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.RPM_FILE;
import static de.is24.infrastructure.gridfs.http.web.RepoTestUtils.uploadRpm;
import static java.lang.Thread.sleep;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.http.entity.ContentType.APPLICATION_FORM_URLENCODED;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.apache.http.entity.mime.HttpMultipartMode.BROWSER_COMPATIBLE;
import static org.apache.http.util.EntityUtils.consume;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertFalse;


@RunWith(LocalOrRemoteDeploymentTestRunner.class)
public class RepositoryControllerIT extends AbstractContainerAndMongoDBStarter {
  private String repoUrl;
  private String reponame;
  private RepoEntriesRepository repoEntriesRepository;

  @Before
  public void setUp() throws Exception {
    reponame = uniqueRepoName();
    repoUrl = deploymentURL + "/repo/" + reponame;

    uploadRpm(repoUrl, RPM_FILE.getPath());

    repoEntriesRepository = new MongoRepositoryFactory(mongoTemplate(mongo)).getRepository(RepoEntriesRepository.class);
  }

  @Test
  public void shouldNotBeAbleToUploadToRootRepo() throws IOException {
    HttpResponse response = uploadRpm(deploymentURL + "/repo/", RPM_FILE.getPath());
    Assertions.assertThat(response.getStatusLine().getStatusCode()).isEqualTo(406);
  }

  @Test
  public void hasStatusCreatedAfterUploadRpmWithMultipartFormData() throws Exception {
    MultipartEntity entity = new MultipartEntity(BROWSER_COMPATIBLE);
    entity.addPart("rpmFile", new FileBody(RPM_FILE));
    thenStatusCreatedForEntity(entity);
    thenMaxKeepRpmsHasDefaultValue(reponame);
  }

  @Test
  public void hasStatusCreatedAfterUploadRpmWithUrlEncoded() throws Exception {
    thenStatusCreatedForEntity(new FileEntity(RPM_FILE, APPLICATION_FORM_URLENCODED));
    thenMaxKeepRpmsHasDefaultValue(reponame);
  }

  @Test
  public void deleteRepository() throws Exception {
    HttpDelete delete = new HttpDelete(repoUrl);
    HttpResponse response = httpClient.execute(delete);
    consume(response.getEntity());
    assertThat(response.getStatusLine().getStatusCode(), is(SC_NO_CONTENT));

    sleep(1000);

    HttpGet get = new HttpGet(repoUrl + "/noarch/test-artifact-1.2-1.noarch.rpm");
    response = httpClient.execute(get);
    assertThat(response.getStatusLine().getStatusCode(), is(SC_NOT_FOUND));
  }

  @Test
  public void modifyRepoType() throws Exception {
    RepoEntry repoEntry = updateRepoEntry("/type", "\"SCHEDULED\"");
    assertThat(repoEntry, notNullValue());
    assertThat(repoEntry.getType(), is(SCHEDULED));
  }

  @Test
  public void modifyMaxKeepRpms() throws Exception {
    RepoEntry repoEntry = updateRepoEntry("/maxKeepRpms", "3");
    assertThat(repoEntry, notNullValue());
    assertThat(repoEntry.getMaxKeepRpms(), is(3));
  }

  @Test
  public void createStaticRepo() throws Exception {
    String reponame = uniqueRepoName();
    HttpPost post = new HttpPost(deploymentURL + "/repo/");
    post.setEntity(new StringEntity("name=" + reponame, (ContentType) null));

    HttpResponse response = httpClient.execute(post);

    consume(response.getEntity());
    assertThat(response.getStatusLine().getStatusCode(), is(SC_CREATED));

    assertFalse(isEmpty(repoEntriesRepository.findByName(reponame)));
    thenMaxKeepRpmsHasDefaultValue(reponame);
  }

  private RepoEntry updateRepoEntry(String propertyUrl, String entity) throws IOException {
    HttpPut put = new HttpPut(repoUrl + propertyUrl);
    put.setEntity(new StringEntity(entity, APPLICATION_JSON));

    HttpResponse response = httpClient.execute(put);
    consume(response.getEntity());
    assertThat(response.getStatusLine().getStatusCode(), is(SC_NO_CONTENT));

    RepoEntriesRepository repoEntriesRepository = new MongoRepositoryFactory(mongoTemplate(mongo)).getRepository(
      RepoEntriesRepository.class);
    return repoEntriesRepository.findFirstByName(reponame);
  }

  private void thenMaxKeepRpmsHasDefaultValue(String reponame) {
    Matcher<RepoEntry> maxKeepRpms = havingValue(on(RepoEntry.class).getMaxKeepRpms(),
      is(RepoEntry.DEFAULT_MAX_KEEP_RPMS));
    assertThat(repoEntriesRepository.findByName(reponame), hasItem(maxKeepRpms));
  }


  private void thenStatusCreatedForEntity(HttpEntity entity) throws IOException {
    HttpPost post = new HttpPost(deploymentURL + "/repo/" + uniqueRepoName());
    post.setEntity(entity);

    HttpResponse response = httpClient.execute(post);
    consume(response.getEntity());

    assertThat(response.getStatusLine().getStatusCode(), is(SC_CREATED));
  }

}
