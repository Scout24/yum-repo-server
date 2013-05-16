package de.is24.infrastructure.gridfs.http.web.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import de.is24.infrastructure.gridfs.http.domain.Container;
import de.is24.infrastructure.gridfs.http.domain.FileInfo;
import de.is24.infrastructure.gridfs.http.domain.FolderInfo;
import de.is24.infrastructure.gridfs.http.web.AbstractContainerAndMongoDBStarter;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Matcher;
import org.junit.Test;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


public abstract class RepositoryInfoControllerIT extends AbstractContainerAndMongoDBStarter {
  protected String givenRepoUrl;
  protected String givenReponame;
  protected String givenRepoListUrl;
  protected String givenRepoUrlWithNoarch;
  protected String givenUnknownRepoUrl;
  protected String givenStaticReponame;
  protected String givenTagName;

  @Test
  public void shouldGetRepos() throws Exception {
    HttpGet get = new HttpGet(givenRepoListUrl);
    get.setHeader("Accept", APPLICATION_JSON_VALUE);

    HttpResponse response = httpClient.execute(get);
    Container<FolderInfo> folderInfoContainer = readJson(response, new TypeReference<Container<FolderInfo>>() {
      });
    assertThat(response.getStatusLine().getStatusCode(), is(SC_OK));
    assertThat(folderInfoContainer.getItems(), hasItem(withSizeGreaterZeroAndName(givenReponame)));
  }

  private CustomMatcher<FolderInfo> withSizeGreaterZeroAndName(final String expectedFolderInfoName) {
    return new CustomMatcher<FolderInfo>("FolderInfo with size greater 0") {
      @Override
      public boolean matches(Object o) {
        if (o instanceof FolderInfo) {
          FolderInfo info = (FolderInfo) o;
          return (info.getSize() > 0) && expectedFolderInfoName.equals(info.getName());
        }
        return false;
      }

    };
  }

  @Test
  public void shouldGetSetOfFilesForGivenRepositoryAndArch() throws Exception {
    HttpGet get = new HttpGet(givenRepoUrlWithNoarch);
    get.setHeader("Accept", APPLICATION_JSON_VALUE);

    HttpResponse response = httpClient.execute(get);
    Container<FileInfo> fileInfoContainer = readJson(response, new TypeReference<Container<FileInfo>>() {
      });

    Set<FileInfo> fileInfos = fileInfoContainer.getItems();
    final Matcher<FileInfo> hasGivenFilename = hasProperty("filename", is("test-artifact-1.2-1.noarch.rpm"));
    final Matcher<FileInfo> hasGivenReponame = hasProperty("repo", is(givenStaticReponame));
    final Matcher<FileInfo> hasNoarch = hasProperty("arch", is("noarch"));

    assertThat(fileInfos, hasItem(allOf(hasNoarch, hasGivenFilename, hasGivenReponame)));
    assertThat(response.getStatusLine().getStatusCode(), is(SC_OK));
    assertThat(givenRepoUrlWithNoarch, endsWith(fileInfoContainer.getPath()));
  }

  @Test
  public void shouldGetSetOfArchsForGivenRepository() throws IOException {
    HttpGet get = new HttpGet(givenRepoUrl);
    get.setHeader("Accept", APPLICATION_JSON_VALUE);

    HttpResponse response = httpClient.execute(get);
    Container<FolderInfo> returnedFolderInfoContainer = readJson(response,
      new TypeReference<Container<FolderInfo>>() {
      });

    assertTrue(returnedFolderInfoContainer.getItems()
      .contains(new FolderInfo(new BasicDBObject("_id", "noarch").append("length", 1L))));
    assertThat(givenRepoUrl, endsWith(returnedFolderInfoContainer.getPath()));
    assertThat(response.getStatusLine().getStatusCode(), is(SC_OK));
  }

  @Test
  public void shouldReturn404ForUnknownRepository() throws IOException {
    HttpGet get = new HttpGet(givenUnknownRepoUrl);
    get.setHeader("Accept", APPLICATION_JSON_VALUE);

    HttpResponse response = httpClient.execute(get);
    assertThat(response.getStatusLine().getStatusCode(), is(HttpServletResponse.SC_NOT_FOUND));
  }

  @Test
  public void provideRepositoryList() throws Exception {
    String repoListAsTextUrl = givenRepoListUrl.substring(0, givenRepoListUrl.length() - 1) + ".json";
    HttpGet get = new HttpGet(repoListAsTextUrl);
    HttpResponse response = httpClient.execute(get);

    final Container<FolderInfo> folderInfoContainer = readJson(response, new TypeReference<Container<FolderInfo>>() {
      });

    final Matcher<FolderInfo> hasGivenRepoName = hasProperty("name", is(givenReponame));
    final Matcher<FolderInfo> hasGivenTagName = hasProperty("tags", hasItem(givenTagName));

    assertThat(folderInfoContainer.getItems(), hasItem(hasGivenRepoName));
    assertThat(response.getStatusLine().getStatusCode(), is(SC_OK));

    if (!givenRepoListUrl.contains("virtual")) {
      assertThat(folderInfoContainer.getItems(), hasItem(hasGivenTagName));
    }
  }

  protected <T> T readJson(HttpResponse response, TypeReference<T> typeReference) throws IOException {
    return new ObjectMapper().readValue(response.getEntity().getContent(), typeReference);
  }
}
