package de.is24.infrastructure.gridfs.http.web.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import de.is24.infrastructure.gridfs.http.domain.Container;
import de.is24.infrastructure.gridfs.http.domain.FolderInfo;
import de.is24.infrastructure.gridfs.http.repos.StaticRepositoryInfoProviderIT;
import de.is24.infrastructure.gridfs.http.web.RepoTestUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.fest.assertions.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.uniqueRepoName;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.RPM_FILE;
import static de.is24.infrastructure.gridfs.http.web.RepoTestUtils.addTagToRepo;
import static de.is24.infrastructure.gridfs.http.web.RepoTestUtils.uploadRpm;
import static de.is24.infrastructure.gridfs.http.web.UrlUtils.join;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.StringContains.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


/**
 * NOTE: Some finders are explicitly tested in {@link StaticRepositoryInfoProviderIT}.
 */
public class StaticRepositoryInfoControllerIT extends RepositoryInfoControllerIT {

  @Before
  public void setUp() throws Exception {
    givenStaticReponame = uniqueRepoName();
    givenReponame = givenStaticReponame;
    givenTagName = "test-tag-name";


    givenRepoListUrl = join(deploymentURL , "/repo/");
    givenRepoUrl = givenRepoListUrl + givenReponame;
    givenRepoUrlWithNoarch = givenRepoUrl + "/noarch";

    givenUnknownRepoUrl = deploymentURL + "/repo/id_do_not_exist";

    uploadRpm(givenRepoUrl, RPM_FILE.getPath());

    addTagToRepo(givenRepoUrl, givenTagName);
  }

  @Test
  public void shouldNotFindReposForQueryStaticByNotMatchingName() throws IOException {
    HttpGet get = new HttpGet(deploymentURL + "/repo.txt?" + "name=" + "notMatchingName");
    HttpResponse response = httpClient.execute(get);

    String content = EntityUtils.toString(response.getEntity());
    assertThat(response.getStatusLine().getStatusCode(), is(HttpServletResponse.SC_OK));
    assertThat(content, not(containsString(givenReponame)));
  }

  @Test
  public void shouldSearchOnTopLevelIfJsonAccepted() throws IOException {
    HttpGet get = new HttpGet(deploymentURL + "/repo/?search=is24-httpd");
    get.setHeader("Accept", APPLICATION_JSON_VALUE);

    HttpResponse response = httpClient.execute(get);

    String content = EntityUtils.toString(response.getEntity());
    assertThat(response.getStatusLine().getStatusCode(), is(HttpServletResponse.SC_OK));
    assertThat(content, containsString("items"));
    assertThat(content, startsWith("{"));

  }

  @Test
  public void shouldSearchInRepoIfJsonAccepted() throws IOException {
    HttpGet get = new HttpGet(deploymentURL + "/repo/testrepo/?search=is24-httpd");
    get.setHeader("Accept", APPLICATION_JSON_VALUE);

    HttpResponse response = httpClient.execute(get);

    String content = EntityUtils.toString(response.getEntity());
    assertThat(response.getStatusLine().getStatusCode(), is(HttpServletResponse.SC_OK));
    assertThat(content, containsString("items"));
    assertThat(content, startsWith("{"));

  }

  @Test
  public void shouldFindReposForQueryStaticByMatchingName() throws IOException {
    HttpGet get = new HttpGet(deploymentURL + "/repo.txt?" + "name=" + givenReponame);
    HttpResponse response = httpClient.execute(get);

    String content = EntityUtils.toString(response.getEntity());
    assertThat(response.getStatusLine().getStatusCode(), is(HttpServletResponse.SC_OK));
    assertThat(content, containsString(givenReponame));
  }

  @Test
  public void shouldFindReposForQueryStaticByMatchingTag() throws IOException {
    HttpGet get = new HttpGet(deploymentURL + "/repo.txt?" + "tag=" + givenTagName);
    HttpResponse response = httpClient.execute(get);

    String content = EntityUtils.toString(response.getEntity());
    assertThat(response.getStatusLine().getStatusCode(), is(HttpServletResponse.SC_OK));
    assertThat(content, containsString(givenReponame));
  }

  @Test
  public void shouldNotFindReposForQueryStaticByNotMatchingTag() throws IOException {
    HttpGet get = new HttpGet(deploymentURL + "/repo.txt?" + "tag=" + "notMatchingTag");
    HttpResponse response = httpClient.execute(get);

    String content = EntityUtils.toString(response.getEntity());
    assertThat(response.getStatusLine().getStatusCode(), is(HttpServletResponse.SC_OK));
    assertThat(content, not(containsString(givenReponame)));
  }

  @Test
  public void shouldFindReposForQueryStaticByMatchingNotag() throws IOException {
    HttpGet get = new HttpGet(deploymentURL + "/repo.txt?" + "notag=" + "notMatchingTag");
    HttpResponse response = httpClient.execute(get);

    String content = EntityUtils.toString(response.getEntity());
    assertThat(response.getStatusLine().getStatusCode(), is(HttpServletResponse.SC_OK));
    assertThat(content, containsString(givenReponame));
  }

  @Test
  public void shouldFindReposForQueryStaticByMatchingNewerDate() throws IOException {
    HttpGet get = new HttpGet(deploymentURL + "/repo.txt?" + "newer=" + 1);
    HttpResponse response = httpClient.execute(get);

    String content = EntityUtils.toString(response.getEntity());
    assertThat(response.getStatusLine().getStatusCode(), is(HttpServletResponse.SC_OK));
    assertThat(content, containsString(givenReponame));
  }

  @Test
  public void shouldNotFindReposForQueryStaticByMatchingNewerAndOlderDate() throws IOException {
    HttpGet get = new HttpGet(deploymentURL + "/repo.txt?" + "newer=" + 5 + "&older=" + 1);
    HttpResponse response = httpClient.execute(get);

    String content = EntityUtils.toString(response.getEntity());
    assertThat(response.getStatusLine().getStatusCode(), is(HttpServletResponse.SC_OK));
    assertThat(content, not(containsString(givenReponame)));
  }

  @Test
  public void shouldGetEmptyRepo() throws IOException {
    String repositoryName = "test" + System.currentTimeMillis();

    HttpResponse responseCreateRepo = RepoTestUtils.createRepository(deploymentURL.toString(), repositoryName);
    assertThat(responseCreateRepo.getStatusLine().getStatusCode(), is(HttpServletResponse.SC_CREATED));

    String repoUrl = deploymentURL + "/repo/" + repositoryName;
    HttpGet get = new HttpGet(repoUrl);
    get.setHeader("Accept", APPLICATION_JSON_VALUE);

    HttpResponse response = httpClient.execute(get);
    Container<FolderInfo> returnedFolderInfoContainer = readJson(response,
      new TypeReference<Container<FolderInfo>>() {
      });

    Assertions.assertThat(returnedFolderInfoContainer.getItems()).isEmpty();
    Assertions.assertThat(returnedFolderInfoContainer.getTotalSize()).isEqualTo(0L);

    assertThat(repoUrl, endsWith(returnedFolderInfoContainer.getPath()));
    assertThat(response.getStatusLine().getStatusCode(), is(SC_OK));
  }
}
