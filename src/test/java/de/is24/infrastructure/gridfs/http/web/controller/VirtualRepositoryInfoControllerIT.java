package de.is24.infrastructure.gridfs.http.web.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import de.is24.infrastructure.gridfs.http.domain.Container;
import de.is24.infrastructure.gridfs.http.domain.FolderInfo;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Matcher;
import org.jboss.arquillian.junit.LocalOrRemoteDeploymentTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.givenVirtualRepo;
import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.givenVirtualRepoLinkedToStatic;
import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.uniqueRepoName;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.RPM_FILE;
import static de.is24.infrastructure.gridfs.http.web.RepoTestUtils.uploadRpm;
import static java.lang.String.format;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


@RunWith(LocalOrRemoteDeploymentTestRunner.class)
public class VirtualRepositoryInfoControllerIT extends RepositoryInfoControllerIT {
  private String staticRepoUrl;

  @Before
  public void setUp() throws Exception {
    givenStaticReponame = uniqueRepoName();
    staticRepoUrl = deploymentURL + "/repo/" + givenStaticReponame;

    uploadRpm(staticRepoUrl, RPM_FILE.getPath());

    String virtualReponame = givenVirtualRepoLinkedToStatic(deploymentURL, givenStaticReponame);
    givenReponame = virtualReponame;

    givenRepoListUrl = deploymentURL + "/repo/virtual/";
    givenRepoUrl = deploymentURL + "/repo/virtual/" + virtualReponame;
    givenRepoUrlWithNoarch = deploymentURL + "/repo/virtual/" + virtualReponame + "/noarch";

    givenUnknownRepoUrl = givenRepoListUrl + "/id_do_not_exist";
  }

  @Test
  public void getExternalRepo() throws Exception {
    String givenHref = "http://some.external/repo";
    givenVirtualRepo(deploymentURL, givenHref);

    HttpGet get = new HttpGet(deploymentURL + "/repo/virtual/");
    get.setHeader("Accept", APPLICATION_JSON_VALUE);

    HttpResponse response = httpClient.execute(get);
    Container<FolderInfo> set = readJson(response, new TypeReference<Container<FolderInfo>>() {
      });
    assertThat(response.getStatusLine().getStatusCode(), is(SC_OK));
    assertThat(set.getItems(), hasItem(withHref(givenHref)));
  }

  @Test
  public void shouldFindReposForQueryVirtualByMatchingName() throws IOException {
    HttpGet get = new HttpGet(deploymentURL + "/repo/virtual.txt?" + "name=" + givenReponame);
    HttpResponse response = httpClient.execute(get);

    String content = EntityUtils.toString(response.getEntity());
    assertThat(response.getStatusLine().getStatusCode(), CoreMatchers.is(HttpServletResponse.SC_OK));
    assertThat(content, containsString(givenReponame));
  }

  @Test
  public void shouldNotFindReposForQueryVirtualByNotMatchingName() throws IOException {
    HttpGet get = new HttpGet(deploymentURL + "/repo/virtual.txt?" + "name=" + "notMatching");
    HttpResponse response = httpClient.execute(get);

    String content = EntityUtils.toString(response.getEntity());
    assertThat(response.getStatusLine().getStatusCode(), CoreMatchers.is(HttpServletResponse.SC_OK));
    assertThat(content, not(containsString(givenReponame)));
  }

  @Test
  public void shouldDisplayTargetForVirtualRepos() throws IOException {
    HttpGet get = new HttpGet(deploymentURL + "/repo/virtual.txt?" + "showDestination=" + "true");
    HttpResponse response = httpClient.execute(get);

    String content = EntityUtils.toString(response.getEntity());
    assertThat(response.getStatusLine().getStatusCode(), CoreMatchers.is(HttpServletResponse.SC_OK));
    assertThat(content, containsString(givenReponame));
    assertThat(content, containsString(givenStaticReponame));
  }

  @Test
  public void shouldNotDisplayTargetForVirtualReposWhenParamIsEmpty() throws IOException {
    HttpGet get = new HttpGet(deploymentURL + "/repo/virtual.txt?" + "showDestination=");
    HttpResponse response = httpClient.execute(get);

    String content = EntityUtils.toString(response.getEntity());
    assertThat(response.getStatusLine().getStatusCode(), CoreMatchers.is(HttpServletResponse.SC_OK));
    assertThat(content, containsString(givenReponame));
    assertThat(content, not(containsString(givenStaticReponame)));
  }


  private Matcher<FolderInfo> withHref(final String exptectedHref) {
    return new CustomMatcher<FolderInfo>(format("FolderInfo with href \"%s\"", exptectedHref)) {
      @Override
      public boolean matches(Object o) {
        return (o instanceof FolderInfo) && exptectedHref.equals(((FolderInfo) o).getHref());
      }
    };
  }


}
