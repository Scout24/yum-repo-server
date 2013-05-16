package de.is24.infrastructure.gridfs.http.repos;

import de.is24.infrastructure.gridfs.http.category.LocalExecutionOnly;
import de.is24.infrastructure.gridfs.http.domain.RepoEntry;
import de.is24.infrastructure.gridfs.http.domain.YumEntry;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackage;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageLocation;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageVersion;
import de.is24.infrastructure.gridfs.http.mongo.IntegrationTestContext;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;

import static ch.lambdaj.Lambda.extract;
import static ch.lambdaj.Lambda.on;
import static de.is24.infrastructure.gridfs.http.mongo.IntegrationTestContext.mongoTemplate;
import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.uniqueRepoName;
import static java.lang.System.currentTimeMillis;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

@Category(LocalExecutionOnly.class)
public class RepoCleanerIT {

  private static final String NAME1 = "test-artifactus";
  private static final String NAME2 = "artifactus-test";
  private static final String NOARCH = "noarch";
  private static final String SRC = "src";

  private static final YumEntry[] YUM_ENTRIES_TO_KEEP = {
      entry(NAME1, "1.0", "1", NOARCH),
      entry(NAME1, "2.0", "1", NOARCH),
      entry(NAME1, "0.1", "1", SRC),
      entry(NAME1, "0.2", "1", SRC),

      entry(NAME2, "1.1.1", "1", NOARCH),
      entry(NAME2, "1.1", "2", NOARCH),
      entry(NAME2, "1.1", "1", NOARCH)};

  private static final YumEntry[] YUM_ENTRIES_TO_CLEAN_UP = {
      entry(NAME2, "1.0","1",NOARCH),
      entry(NAME2, "0.9","1",NOARCH)};

  @ClassRule
  public static IntegrationTestContext context = new IntegrationTestContext();
  private String reponame;
  private RepoCleaner service;

  @Before
  public void setUp() throws Exception {
    reponame = uniqueRepoName();
    service = new RepoCleaner(mongoTemplate(context.getMongo()), context.yumEntriesRepository(), context.gridFs(), context.repoService());
  }

  @Test
  public void cleanupRepo() throws Exception {
    givenRepoWithFilesToClean();
    long startTime = currentTimeMillis();

    assertThat(service.cleanup(reponame, 3), is(true));

    assertThatItemsHasBeenCleanedUp();
    assertThatRepoEntryIsMarkedAsModified(startTime);
  }

  @Test
  public void cleanupRepoDirectly() throws Exception {
    givenRepoWithFilesToClean();
    givenRepoEntryWithMaxKeep(3);
    long startTime = currentTimeMillis();

    assertThat(service.cleanup(reponame), is(true));

    assertThatItemsHasBeenCleanedUp();
    assertThatRepoEntryIsMarkedAsModified(startTime);
  }

  @Test
  public void doNothingIfMaxkeepRPMsIsZero() throws Exception {
    givenRepoWithFilesToClean();
    
    assertThat(service.cleanup(reponame, 0), is(false));
    assertThat(service.cleanup(reponame), is(false));

    assertThatNoItemsHasBeenCleanedUp();
  }

  private void assertThatRepoEntryIsMarkedAsModified(long startTime) {
    RepoEntry repoEntry = context.repoEntriesRepository().findFirstByName(reponame);
    assertThat(repoEntry.getLastModified().getTime(), greaterThan(startTime));
  }

  private void assertThatItemsHasBeenCleanedUp() {
    assertEntriesExist(YUM_ENTRIES_TO_KEEP);
    for (YumEntry entry : YUM_ENTRIES_TO_CLEAN_UP) {
      assertEntryDoesNotExist(entry);
    }
  }

  private void assertThatNoItemsHasBeenCleanedUp() {
    assertEntriesExist(YUM_ENTRIES_TO_KEEP);
    assertEntriesExist(YUM_ENTRIES_TO_CLEAN_UP);
  }

  private void assertEntriesExist(YumEntry[] entries) {
    for (YumEntry entry : entries) {
      assertEntryExists(entry);
    }
  }

  private void assertEntryDoesNotExist(YumEntry entry) {
    assertThat(findVersionsFor(entry.getYumPackage().getName()), not(hasItem(entry.getYumPackage().getVersion())));
  }

  private void assertEntryExists(YumEntry entry) {
    assertThat(findVersionsFor(entry.getYumPackage().getName()), hasItem(entry.getYumPackage().getVersion()));
  }

  private List<YumPackageVersion> findVersionsFor(String artifactName) {
    List<YumEntry> entries = context.yumEntriesRepository().findByRepoAndYumPackageName(reponame, artifactName);
    List<YumPackage> yumPackages = extract(entries, on(YumEntry.class).getYumPackage());
    return extract(yumPackages, on(YumPackage.class).getVersion());
  }

  private void givenRepoEntryWithMaxKeep(int maxKeepRpms) {
    RepoEntry repoEntry = context.repoService().ensureEntry(reponame, null);
    repoEntry.setMaxKeepRpms(maxKeepRpms);
    context.repoEntriesRepository().save(repoEntry);
  }

  private void givenRepoWithFilesToClean() {
    context.repoService().setMaxKeepRpms(reponame,0);
    for (YumEntry entry : YUM_ENTRIES_TO_KEEP) {
      entry.setRepo(reponame);
      context.yumEntriesRepository().save(entry);
    }

    for (YumEntry entry : YUM_ENTRIES_TO_CLEAN_UP) {
      entry.setRepo(reponame);
      context.yumEntriesRepository().save(entry);
    }
  }

  private static YumEntry entry(String name, String version, String release, String arch) {
    YumPackage yumPackage = new YumPackage();
    yumPackage.setName(name);
    yumPackage.setArch(arch);
    yumPackage.setVersion(packageVersion(version, release));
    YumPackageLocation location = new YumPackageLocation();
    location.setHref(arch + "/" + name + "-" + version + "-" + release + "." + arch + ".rpm");
    yumPackage.setLocation(location);
    return new YumEntry(null, null, yumPackage);
  }

  private static YumPackageVersion packageVersion(String version, String release) {
    YumPackageVersion packageVersion = new YumPackageVersion();
    packageVersion.setEpoch(0);
    packageVersion.setVer(version);
    packageVersion.setRel(release);
    return packageVersion;
  }
}
