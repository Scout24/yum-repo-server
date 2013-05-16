package de.is24.infrastructure.gridfs.http.repos;

import de.is24.infrastructure.gridfs.http.domain.RepoEntry;
import de.is24.infrastructure.gridfs.http.exception.RepositoryNotFoundException;
import de.is24.infrastructure.gridfs.http.metadata.RepoEntriesRepository;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author twalter
 * @since 4/8/13
 */
public class RepoTaggingServiceTest {

  public static final String REPO_NAME = "repoName";
  public static final String TAG = "repoTag";
  public static final String TAG2 = "repoTag2";
  private RepoEntriesRepository repoEntriesRepository;
  private RepoTaggingService repoTaggingService;

  @Before
  public void setUp() {
    repoEntriesRepository = mock(RepoEntriesRepository.class);
    repoTaggingService = new RepoTaggingService(repoEntriesRepository);

  }

  @Test
  public void addTag() {
    when(repoEntriesRepository.findFirstByName(REPO_NAME)).thenReturn(givenRepoEntry());
    repoTaggingService.addTag(REPO_NAME, TAG);

    RepoEntry repoEntry = givenRepoEntry();
    repoEntry.getTags().add(TAG);

    verify(repoEntriesRepository).save(eq(repoEntry));
  }

  @Test(expected = RepositoryNotFoundException.class)
  public void failWhenRepositoryNotFound() {
    repoTaggingService.addTag(REPO_NAME, TAG);
  }

  @Test
  public void getTags() {
    when(repoEntriesRepository.findFirstByName(REPO_NAME)).thenReturn(givenRepoEntry());
    repoTaggingService.addTag(REPO_NAME, TAG);
    repoTaggingService.addTag(REPO_NAME, TAG2);

    Set<String> tags = repoTaggingService.getTags(REPO_NAME);
    assertThat(tags, hasItem(TAG));
    assertThat(tags, hasItem(TAG2));
  }

  @Test
  public void deleteExistingTagForRepo() {
    RepoEntry repoEntry = givenRepoEntry();
    repoEntry.getTags().add(TAG);
    repoEntry.getTags().add(TAG2);
    
    when(repoEntriesRepository.findFirstByName(REPO_NAME)).thenReturn(repoEntry);
    
    repoTaggingService.deleteTag(REPO_NAME,TAG2);

    RepoEntry expectedRepoEntry = givenRepoEntry();
    expectedRepoEntry.getTags().add(TAG);
    
    verify(repoEntriesRepository).save(eq(expectedRepoEntry));
  }

  private RepoEntry givenRepoEntry() {
    RepoEntry repoEntry = new RepoEntry();
    repoEntry.setName(REPO_NAME);

    return repoEntry;
  }

}
