package de.is24.infrastructure.gridfs.http.metadata;

import de.is24.infrastructure.gridfs.http.domain.YumEntry;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackage;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageChecksum;
import de.is24.infrastructure.gridfs.http.mongo.IntegrationTestContext;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.uniqueRepoName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


public class YumEntriesHashCalculatorIT {
  private static final String AB_MD5 = "187ef4436122d1cc2f40dc2b92f0eba0";
  private static final String CBA_MD5 = "3944b025c9ca7eec3154b44666ae04a0";

  @ClassRule
  public static IntegrationTestContext context = new IntegrationTestContext();

  private YumEntriesHashCalculator yumEntriesHashCalculator;
  private String repoName;

  @Before
  public void setUp() throws Exception {
    yumEntriesHashCalculator = new YumEntriesHashCalculator(context.mongoTemplate());
    repoName = uniqueRepoName();
  }

  @Test
  public void noEntriesHashToEmpty() throws Exception {
    final String hash = yumEntriesHashCalculator.hashForRepo(repoName);
    assertThat(hash, is(""));
  }

  @Test
  public void hashOfEntryHashes() throws Exception {
    givenEntriesWithHash("a", "b");

    final String hash = yumEntriesHashCalculator.hashForRepo(repoName);

    assertThat(hash, is(AB_MD5));
  }

  @Test
  public void hashOfEntryHashes2() throws Exception {
    givenEntriesWithHash("c", "b", "a");

    final String hash = yumEntriesHashCalculator.hashForRepo(repoName);

    assertThat(hash, is(CBA_MD5));
  }

  private void givenEntriesWithHash(String... hashes) {
    for (String hash : hashes) {
      final YumPackage yumPackage = new YumPackage();
      yumPackage.setChecksum(new YumPackageChecksum("egal", hash));
      context.yumEntriesRepository().save(new YumEntry(new ObjectId(), repoName, yumPackage));
    }
  }
}
