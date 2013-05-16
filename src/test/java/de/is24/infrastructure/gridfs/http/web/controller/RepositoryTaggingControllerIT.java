package de.is24.infrastructure.gridfs.http.web.controller;

import static de.is24.infrastructure.gridfs.http.mongo.IntegrationTestContext.mongoTemplate;
import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.uniqueRepoName;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.RPM_FILE;
import static de.is24.infrastructure.gridfs.http.web.RepoTestUtils.uploadRpm;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static org.apache.http.util.EntityUtils.consume;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.hamcrest.collection.IsEmptyCollection;
import org.jboss.arquillian.junit.LocalOrRemoteDeploymentTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import de.is24.infrastructure.gridfs.http.domain.RepoEntry;
import de.is24.infrastructure.gridfs.http.metadata.RepoEntriesRepository;
import de.is24.infrastructure.gridfs.http.web.AbstractContainerAndMongoDBStarter;


/**
 * @author twalter
 * @since 4/8/13
 */
@RunWith(LocalOrRemoteDeploymentTestRunner.class)
public class RepositoryTaggingControllerIT extends AbstractContainerAndMongoDBStarter {
  private String reponame;
  private String repoUrl;
  private static final String TAGNAME = "repoTagName";
  private static final String TAGNAME2 = "anotherTagName";

  @Before
  public void setUp() throws Exception {
    reponame = uniqueRepoName();
    repoUrl = deploymentURL + "/repo/" + reponame;

    uploadRpm(repoUrl, RPM_FILE.getPath());
  }

  @Test
  public void addTagToExistingRepo() throws IOException {
    givenCreatedTag(TAGNAME);
    thenRepoHasTag(TAGNAME);
  }

  @Test
  public void showTags() throws IOException {
    givenCreatedTag(TAGNAME);
    givenCreatedTag(TAGNAME2);

    HttpGet get = new HttpGet(repoUrl + "/tags/");
    final HttpResponse response = httpClient.execute(get);
    assertThat(response.getStatusLine().getStatusCode(), is(200));

    String content = EntityUtils.toString(response.getEntity());
    assertThat(content, containsString(TAGNAME));
    assertThat(content, containsString(TAGNAME2));
  }

  @Test
  public void deleteTagFromExistingRepo() throws IOException {
    givenCreatedTag(TAGNAME);
    givenCreatedTag(TAGNAME2);

    whenRepoTagIsDeletedFor(TAGNAME2);

    thenRepoHasTag(TAGNAME);
    thenRepoHasNotTag(TAGNAME2);
  }

  private void whenRepoTagIsDeletedFor(String tagName) throws IOException {
    HttpDelete post = new HttpDelete(repoUrl + "/tags/" + tagName);

    final HttpResponse response = httpClient.execute(post);
    consume(response.getEntity());

    assertThat(response.getStatusLine().getStatusCode(), is(SC_NO_CONTENT));
  }

  @Test
  public void deleteAllTagsFromExistingRepo() throws IOException {
    givenCreatedTag(TAGNAME);
    givenCreatedTag(TAGNAME2);

    whenAllRepoTagsDeletedFor();

    RepoEntry repoEntry = firstRepoEntryFromRepo();
    assertThat(repoEntry.getTags(), IsEmptyCollection.<String>empty());
  }

  private void whenAllRepoTagsDeletedFor() throws IOException {
    HttpDelete post = new HttpDelete(repoUrl + "/tags");

    final HttpResponse response = httpClient.execute(post);
    consume(response.getEntity());

    assertThat(response.getStatusLine().getStatusCode(), is(SC_NO_CONTENT));
  }

  private void givenCreatedTag(String tagname) throws IOException {
    HttpPost post = new HttpPost(repoUrl + "/tags/");
    post.setEntity(new StringEntity("tag=" + tagname, (ContentType) null));

    final HttpResponse response = httpClient.execute(post);
    consume(response.getEntity());

    assertThat(response.getStatusLine().getStatusCode(), is(SC_CREATED));
  }

  private void thenRepoHasTag(String tagName) {
    RepoEntry repoEntry = firstRepoEntryFromRepo();

    assertThat(repoEntry.getTags(), hasItem(tagName));
  }

  private void thenRepoHasNotTag(String tagName) {
    RepoEntry repoEntry = firstRepoEntryFromRepo();

    assertThat(repoEntry.getTags(), not(hasItem(tagName)));
  }

  private RepoEntry firstRepoEntryFromRepo() {
    RepoEntriesRepository repoEntriesRepository = new MongoRepositoryFactory(mongoTemplate(mongo)).getRepository(
      RepoEntriesRepository.class);
    return repoEntriesRepository.findFirstByName(reponame);
  }
}
