package de.is24.infrastructure.gridfs.http.repos;

import de.is24.infrastructure.gridfs.http.category.LocalExecutionOnly;
import de.is24.infrastructure.gridfs.http.domain.Container;
import de.is24.infrastructure.gridfs.http.domain.FolderInfo;
import de.is24.infrastructure.gridfs.http.domain.RepoEntry;
import de.is24.infrastructure.gridfs.http.domain.SortField;
import de.is24.infrastructure.gridfs.http.domain.SortOrder;
import de.is24.infrastructure.gridfs.http.gridfs.GridFsService;
import de.is24.infrastructure.gridfs.http.mongo.IntegrationTestContext;
import de.is24.infrastructure.gridfs.http.web.controller.StaticRepositoryInfoControllerIT;
import org.fest.assertions.api.Assertions;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import java.util.Date;
import java.util.List;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.function.matcher.HasArgumentWithValue.havingValue;
import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.uniqueRepoName;
import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.uniqueRepoNameWithPrefix;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.COMPLEX_RPM_FILE_NAME;
import static de.is24.infrastructure.gridfs.http.utils.RpmUtils.streamOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.joda.time.DateTime.now;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.core.query.Update.update;


/**
 * NOTE: Most finders are tested <b>implicitly</b> in {@link StaticRepositoryInfoControllerIT}.
 */
@Category(LocalExecutionOnly.class)
public class StaticRepositoryInfoProviderIT {
  @ClassRule
  public static IntegrationTestContext context = new IntegrationTestContext();
  private StaticRepositoryInfoProvider provider;
  private GridFsService gridFsService;

  @Before
  public void setUp() throws Exception {
    provider = new StaticRepositoryInfoProvider(context.mongoTemplate(), context.repoEntriesRepository());
    gridFsService = context.gridFsService();
    cleanExistingRepos();
  }

  private void cleanExistingRepos() {
    final List<RepoEntry> repoEntries = context.repoEntriesRepository().findAll();
    for (RepoEntry entry : repoEntries) {
      gridFsService.deleteRepository(entry.getName());
    }
    context.gridFsTemplate().delete(null);
  }

  @Test
  public void shouldFindReposForQueryStaticByMatchingRegex() throws Exception {
    String givenReponameRegex = "r.po-[0-9]*";
    String reponame = createRepoFromDaysAgoWithData(0);

    List<RepoEntry> entries = findRepoByNameAndOlderDays(givenReponameRegex, 0);
    assertResultContainsRepoWithGivenName(reponame, entries);
    cleanUpRepositories(reponame);
  }

  @Test
  public void shouldFindReposForQueryStaticByMatchingOlderDate() throws Exception {
    String givenReponame = createRepoFromDaysAgoWithData(2);

    List<RepoEntry> entries = findRepoByNameAndOlderDays(".", 1);

    assertResultContainsRepoWithGivenName(givenReponame, entries);

    cleanUpRepositories(givenReponame);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void shouldGetEmptyAndNotEmptyRepos() throws Exception {
    String givenRepoWithData = createRepoFromDaysAgoWithData(0);
    String givenEmptyRepo = createEmptyRepoWithPrefix("emptyRepo");

    Container<FolderInfo> staticRepos = provider.getRepos(SortField.name, SortOrder.asc);

    Matcher<FolderInfo> folderInfoWithRepoWithData = havingValue(on(FolderInfo.class).getName(), is(givenRepoWithData));
    Matcher<FolderInfo> folderInfoWithEmptyRepo = havingValue(on(FolderInfo.class).getName(), is(givenEmptyRepo));

    assertThat(staticRepos.getItems(), hasItems(folderInfoWithEmptyRepo, folderInfoWithRepoWithData));

    cleanUpRepositories(givenEmptyRepo, givenRepoWithData);
  }

  @Test
  public void shouldGetSortedStaticReposByName() throws Exception {
    String givenEmptyRepo1 = createEmptyRepoWithPrefix("A");
    String givenEmptyRepo2 = createEmptyRepoWithPrefix("B");
    String givenEmptyRepo3 = createEmptyRepoWithPrefix("C");

    Container<FolderInfo> sortedStaticRepos = provider.getRepos(SortField.name, SortOrder.asc);
    thenReposAreSortedByName(sortedStaticRepos, givenEmptyRepo1, givenEmptyRepo2, givenEmptyRepo3);

    sortedStaticRepos = provider.getRepos(SortField.name, SortOrder.desc);
    thenReposAreSortedByName(sortedStaticRepos, givenEmptyRepo3, givenEmptyRepo2, givenEmptyRepo1);

    cleanUpRepositories(givenEmptyRepo1, givenEmptyRepo2, givenEmptyRepo3);
  }

  @Test
  public void shouldHaveNotRootAsRepoInList() throws Exception {
    Container<FolderInfo> repos = provider.getRepos(SortField.name, SortOrder.asc);

    for (FolderInfo info : repos.getItems()) {
      Assertions.assertThat(info.getName()).isNotNull();
    }
  }

  private String createEmptyRepoWithPrefix(String prefix) {
    String reponame = uniqueRepoNameWithPrefix(prefix);
    context.repoService().createOrUpdate(reponame);
    return reponame;
  }

  private String createRepoFromDaysAgoWithData(int days) throws Exception {
    String reponame = uniqueRepoName();
    context.gridFsService().storeRpm(reponame, streamOf(COMPLEX_RPM_FILE_NAME));
    context.metadataService().generateYumMetadata(reponame);
    context.mongoTemplate()
    .updateFirst(
      query(where("name").is(reponame)), update("lastModified", now().minusDays(days).toDate()),
      RepoEntry.class);
    return reponame;
  }

  private List<RepoEntry> findRepoByNameAndOlderDays(String givenReponameRegex, int olderdays) {
    Date newerSevenDaysAgo = now().minusDays(7).toDate();
    Date olderOneDayAgo = now().minusDays(olderdays).toDate();
    return provider.find(givenReponameRegex, newerSevenDaysAgo, olderOneDayAgo);
  }

  @SuppressWarnings("unchecked")
  private void thenReposAreSortedByName(Container<FolderInfo> givenStaticRepos, String expectedValue1,
                                        String expectedValue2, String expectedValue3) {
    Matcher<FolderInfo> matcher1 = havingValue(on(FolderInfo.class).getName(), is(expectedValue1));
    Matcher<FolderInfo> matcher2 = havingValue(on(FolderInfo.class).getName(), is(expectedValue2));
    Matcher<FolderInfo> matcher3 = havingValue(on(FolderInfo.class).getName(), is(expectedValue3));

    assertThat(givenStaticRepos.getItems(), contains(matcher1, matcher2, matcher3));
  }

  private void assertResultContainsRepoWithGivenName(String reponame, List<RepoEntry> entries) {
    Matcher<RepoEntry> entryWithGivenName = havingValue(on(RepoEntry.class).getName(), is(reponame));
    assertThat(entries, hasItem(entryWithGivenName));
  }

  private void cleanUpRepositories(String... repoNames) {
    for (String repoName : repoNames) {
      gridFsService.deleteRepository(repoName);
    }
  }
}
