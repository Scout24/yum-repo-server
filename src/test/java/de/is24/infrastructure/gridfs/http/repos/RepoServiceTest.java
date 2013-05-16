package de.is24.infrastructure.gridfs.http.repos;

import de.is24.infrastructure.gridfs.http.domain.RepoEntry;
import de.is24.infrastructure.gridfs.http.exception.BadRequestException;
import de.is24.infrastructure.gridfs.http.exception.RepositoryNotFoundException;
import de.is24.infrastructure.gridfs.http.metadata.RepoEntriesRepository;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static de.is24.infrastructure.gridfs.http.domain.RepoType.SCHEDULED;
import static de.is24.infrastructure.gridfs.http.domain.RepoType.STATIC;
import static de.is24.infrastructure.gridfs.http.domain.RepoType.VIRTUAL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RepoServiceTest {
  private static final Date BEFORE = new Date();
  private static final Date AFTER = new Date(BEFORE.getTime() + 10000);
  private static final String ANY_REPONAME = "any-reponame";
  public static final String EXTERNAL_URL = "http://any.domain/path/";

  private RepoEntriesRepository repository;
  private RepoService service;

  @Before
  public void setup() {
    this.repository = mock(RepoEntriesRepository.class);
    this.service = new RepoService(repository);
  }

  @Test
  public void needsMetadataUpdate() throws Exception {
    assertUpdate(null, null, true);
    assertUpdate(null, AFTER, true);
    assertUpdate(AFTER, null, true);
    assertUpdate(AFTER, BEFORE, true);
    assertUpdate(BEFORE, AFTER, false);
  }

  @Test
  public void callRepoEntriesRepositoryWithReponame() {
    when(repository.findFirstByName(ANY_REPONAME)).thenReturn(createRepoEntry(false));

    service.isRepoScheduled(ANY_REPONAME);

    verify(repository).findFirstByName(ANY_REPONAME);
  }

  @Test
  public void returnsFalseIfRepoIsNotScheduled() {
    when(repository.findFirstByName(ANY_REPONAME)).thenReturn(createRepoEntry(false));

    assertThat(service.isRepoScheduled(ANY_REPONAME), equalTo(false));
  }

  @Test
  public void returnsTrueIfRepoIsScheduled() {
    when(repository.findFirstByName(ANY_REPONAME)).thenReturn(createRepoEntry(true));

    assertThat(service.isRepoScheduled(ANY_REPONAME), equalTo(true));
  }

  @Test
  public void returnsFalseIfRepoIsNotFound() {
    when(repository.findFirstByName(ANY_REPONAME)).thenReturn(createRepoEntry(false));

    assertThat(service.isRepoScheduled(ANY_REPONAME), equalTo(false));
  }

  @Test
  public void updateLastMetadataGeneration() throws Exception {
    Date date = new Date();
    RepoEntry entry = new RepoEntry();
    entry.setLastMetadataGeneration(date);
    entry.setType(STATIC);
    entry.setName(ANY_REPONAME);
    entry.setLastModified(new Date(date.getTime() - 1000));
    service.updateLastMetadataGeneration(ANY_REPONAME, date);

    verify(repository).save(entry);
  }

  @Test
  public void activateSchedulingForRepo() throws Exception {
    RepoEntry repoEntry = createRepoEntry(false);
    when(repository.findFirstByName(ANY_REPONAME)).thenReturn(repoEntry);

    service.activateSchedulingForRepo(ANY_REPONAME);

    assertThat(repoEntry.getType(), is(SCHEDULED));
    verify(repository).save(eq(repoEntry));
  }

  @Test(expected = BadRequestException.class)
  public void failForInvalidReponame() throws Exception {
    when(repository.findFirstByNameAndType("found", STATIC)).thenReturn(createRepoEntry(false));
    service.createVirtualRepo("?name", "static/found");
  }

  @Test(expected = BadRequestException.class)
  public void throwExceptionIfStaticTargetRepoNotFound() throws Exception {
    service.createVirtualRepo(ANY_REPONAME, "static/not-found");
  }

  @Test(expected = BadRequestException.class)
  public void throwExceptionForInvalidUrl() throws Exception {
    service.createVirtualRepo(ANY_REPONAME, "invalid::///url:/");
  }

  @Test(expected = IllegalArgumentException.class)
  public void throwExceptionIfExistingStaticRepoWithSameName() throws Exception {
    when(repository.findFirstByName(ANY_REPONAME)).thenReturn(createRepoEntry(false));
    service.ensureEntry(ANY_REPONAME, SCHEDULED);
  }

  @Test
  public void ensureEntry() throws Exception {
    RepoEntry repoEntry = service.ensureEntry(ANY_REPONAME, null);
    assertThat(repoEntry.getName(), is(ANY_REPONAME));
    assertThat(repoEntry.getType(), is(STATIC));
  }

  @Test(expected = BadRequestException.class)
  public void failForInvalidReponameInEnsureEntry() throws Exception {
    service.ensureEntry("?.-456456()", null);
  }


  @Test
  public void saveVirtualRepoLinkedToStatic() throws Exception {
    RepoEntry staticEntry = createRepoEntry(false);
    when(repository.findFirstByName("static-repo")).thenReturn(staticEntry);
    service.createVirtualRepo(ANY_REPONAME, "static/static-repo");

    RepoEntry virtualEntry = new RepoEntry();
    virtualEntry.setName(ANY_REPONAME);
    virtualEntry.setType(VIRTUAL);
    virtualEntry.setExternal(false);
    virtualEntry.setTarget("static-repo");
    verify(repository).save(eq(virtualEntry));
  }

  @Test(expected = BadRequestException.class)
  public void failIfVirtualTargetRepositoryNotFound() throws Exception {
    service.createVirtualRepo(ANY_REPONAME, "virtual/not-found");
  }

  @Test
  public void saveVirtualRepoLinkedToVirtual() throws Exception {
    RepoEntry virtualTargetEntry = createRepoEntry(false);
    virtualTargetEntry.setExternal(false);
    virtualTargetEntry.setTarget("static-repo");
    when(repository.findFirstByNameAndType("virtual-repo", VIRTUAL)).thenReturn(virtualTargetEntry);
    service.createVirtualRepo(ANY_REPONAME, "virtual/virtual-repo");

    RepoEntry virtualEntry = new RepoEntry();
    virtualEntry.setName(ANY_REPONAME);
    virtualEntry.setType(VIRTUAL);
    virtualEntry.setExternal(false);
    virtualEntry.setTarget("static-repo");
    verify(repository).save(eq(virtualEntry));
  }

  @Test
  public void saveVirtualRepoLinkedToExternal() throws Exception {
    service.createVirtualRepo(ANY_REPONAME, EXTERNAL_URL);

    RepoEntry virtualEntry = new RepoEntry();
    virtualEntry.setName(ANY_REPONAME);
    virtualEntry.setType(VIRTUAL);
    virtualEntry.setExternal(true);
    virtualEntry.setTarget(EXTERNAL_URL);
    verify(repository).save(eq(virtualEntry));
  }

  @Test(expected = RepositoryNotFoundException.class)
  public void failIfRepositoryNotFound() throws Exception {
    service.getRepo(ANY_REPONAME, STATIC);
  }

  @Test
  public void getRepoEntry() throws Exception {
    RepoEntry entry = createRepoEntry(false);
    when(repository.findFirstByNameAndType(ANY_REPONAME, STATIC)).thenReturn(entry);
    assertThat(service.getRepo(ANY_REPONAME, STATIC), is(entry));
  }

  private RepoEntry createRepoEntry(boolean scheduled) {
    RepoEntry repoEntry = new RepoEntry();
    repoEntry.setType(scheduled ? SCHEDULED : STATIC);
    return repoEntry;
  }

  private void assertUpdate(Date lastModified, Date lastMetadataGeneration, boolean expectedResult) {
    RepoEntry repoEntry = new RepoEntry();
    repoEntry.setLastModified(lastModified);
    repoEntry.setLastMetadataGeneration(lastMetadataGeneration);

    when(repository.findFirstByName(ANY_REPONAME)).thenReturn(repoEntry);
    assertThat(service.needsMetadataUpdate(ANY_REPONAME), is(expectedResult));
  }
}
