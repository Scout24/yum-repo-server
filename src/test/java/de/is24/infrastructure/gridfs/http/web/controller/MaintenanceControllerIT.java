package de.is24.infrastructure.gridfs.http.web.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.is24.infrastructure.gridfs.http.domain.YumEntry;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackage;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageLocation;
import de.is24.infrastructure.gridfs.http.web.boot.AbstractContainerAndMongoDBStarter;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static de.is24.infrastructure.gridfs.http.mongo.IntegrationTestContext.mongoTemplate;
import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.uniqueRepoName;
import static de.is24.infrastructure.gridfs.http.web.RepoTestUtils.uploadRpm;
import static java.util.stream.Collectors.toList;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;


public class MaintenanceControllerIT extends AbstractContainerAndMongoDBStarter {
  public static final TypeReference<Set<YumPackage>> YUM_PACKAGE_TYPE = new TypeReference<Set<YumPackage>>() {
  };
  private String sourceReponame;
  private String targetReponame;
  private static final File MAINTENANCE_RPM_DIR = new File("src/test/maintenancerpms");

  @Before
  public void setUp() throws Exception {
    String reponame = uniqueRepoName();
    sourceReponame = "source" + reponame;
    targetReponame = "target" + reponame;
    String sourceRepoUrl = deploymentURL + "/repo/" + sourceReponame;
    String targetRepoUrl = deploymentURL + "/repo/" + targetReponame;

    uploadRpm(sourceRepoUrl, MAINTENANCE_RPM_DIR.getPath() + "/is24-dummyRpmForTesting-57034-2.noarch.rpm");
    uploadRpm(sourceRepoUrl, MAINTENANCE_RPM_DIR.getPath() + "/is24-dummyRpmForTesting-57035-4.noarch.rpm");
    uploadRpm(sourceRepoUrl, MAINTENANCE_RPM_DIR.getPath() + "/is24-dummyRpmForTesting-57037-6.noarch.rpm");

    uploadRpm(targetRepoUrl, MAINTENANCE_RPM_DIR.getPath() + "/is24-dummyRpmForTesting-57034-3.noarch.rpm");
    uploadRpm(targetRepoUrl, MAINTENANCE_RPM_DIR.getPath() + "/is24-dummyRpmForTesting-57037-5.noarch.rpm");

    uploadRpm(sourceRepoUrl, MAINTENANCE_RPM_DIR.getPath() + "/is24-dummyRpmForTesting-1-1.src.rpm");
    uploadRpm(sourceRepoUrl, MAINTENANCE_RPM_DIR.getPath() + "/is24-dummyRpmForTesting-2-2.src.rpm");
    uploadRpm(sourceRepoUrl, MAINTENANCE_RPM_DIR.getPath() + "/is24-dummyRpmForTesting-5-3.src.rpm");

    uploadRpm(targetRepoUrl, MAINTENANCE_RPM_DIR.getPath() + "/is24-dummyRpmForTesting-1-2.src.rpm");
    uploadRpm(targetRepoUrl, MAINTENANCE_RPM_DIR.getPath() + "/is24-dummyRpmForTesting-3-2.src.rpm");
    uploadRpm(targetRepoUrl, MAINTENANCE_RPM_DIR.getPath() + "/is24-dummyRpmForTesting-4-3.src.rpm");
  }

  @Test
  public void findRPMsThatWillNeverBeInstalledFromSourceIfTargetAlsoRegisterdInClient() throws Exception {
    HttpGet get = new HttpGet(deploymentURL + "/maintenance/obsolete?sourceRepo=" + sourceReponame + "&targetRepo=" +
      targetReponame);
    List<String> hrefs = getResultingHrefs(get);
    assertThat(hrefs.size(), is(4));
    assertThat(hrefs, hasItem("noarch/is24-dummyRpmForTesting-57034-2.noarch.rpm"));
    assertThat(hrefs, hasItem("noarch/is24-dummyRpmForTesting-57035-4.noarch.rpm"));
    assertThat(hrefs, hasItem("src/is24-dummyRpmForTesting-1-1.src.rpm"));
    assertThat(hrefs, hasItem("src/is24-dummyRpmForTesting-2-2.src.rpm"));
  }

  @Test
  public void findRPMsThatMayBePropagatedFromSourceToTarget() throws Exception {
    HttpGet get = new HttpGet(deploymentURL + "/maintenance/propagatable?sourceRepo=" + sourceReponame +
      "&targetRepo=" +
      targetReponame);
    List<String> hrefs = getResultingHrefs(get);
    assertThat(hrefs.size(), is(2));
    assertThat(hrefs, hasItem("noarch/is24-dummyRpmForTesting-57037-6.noarch.rpm"));
    assertThat(hrefs, hasItem("src/is24-dummyRpmForTesting-5-3.src.rpm"));
  }

  @Test
  public void findFilesWithoutYumEntries() throws Exception {
    mongoTemplate(mongo).remove(query(where("repo").is(sourceReponame)), YumEntry.class);

    HttpGet get = new HttpGet(deploymentURL + "/maintenance/consistency/files");
    HttpResponse response = httpClient.execute(get);
    assertThat(response.getStatusLine().getStatusCode(), is(SC_OK));
    String jsonString = EntityUtils.toString(response.getEntity());
    assertThat(jsonString, containsString("is24-dummyRpmForTesting-57034-2.noarch.rpm"));
  }

  private List<String> getResultingHrefs(HttpGet get) throws IOException {
    HttpResponse response = httpClient.execute(get);
    assertThat(response.getStatusLine().getStatusCode(), is(SC_OK));
    return readJsonToSet(response).stream().map(YumPackage::getLocation).map(YumPackageLocation::getHref).collect(toList());
  }


  protected Set<YumPackage> readJsonToSet(HttpResponse response) throws IOException {
    return new ObjectMapper().readValue(response.getEntity().getContent(), YUM_PACKAGE_TYPE);
  }

}
