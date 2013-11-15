package de.is24.infrastructure.gridfs.http.web.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackage;
import de.is24.infrastructure.gridfs.http.web.AbstractContainerAndMongoDBStarter;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.jboss.arquillian.junit.LocalOrRemoteDeploymentTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.uniqueRepoName;
import static de.is24.infrastructure.gridfs.http.web.RepoTestUtils.uploadRpm;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


@RunWith(LocalOrRemoteDeploymentTestRunner.class)
public class MaintenanceControllerIT extends AbstractContainerAndMongoDBStarter {
  private String sourceRepoUrl;
  private String targetRepoUrl;
  private String sourceReponame;
  private String targetReponame;
  private static final File MAINTENANCE_RPM_DIR = new File("src/test/maintenancerpms");

  @Before
  public void setUp() throws Exception {
    String reponame = uniqueRepoName();
    sourceReponame = "source" + reponame;
    targetReponame = "target" + reponame;
    sourceRepoUrl = deploymentURL + "/repo/" + sourceReponame;
    targetRepoUrl = deploymentURL + "/repo/" + targetReponame;

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
    HttpGet get = new HttpGet(deploymentURL + "/maintenance/?sourceRepo=" + sourceReponame + "&targetRepo=" +
      targetReponame);
    HttpResponse response = httpClient.execute(get);

    assertThat(response.getStatusLine().getStatusCode(), is(SC_OK));

    final Set<YumPackage> removableYumPackages = readJson(response, new TypeReference<Set<YumPackage>>() {
      });
    assertThat(removableYumPackages.size(), is(4));

    HashSet<String> hrefs = new HashSet<String>();
    Iterator<YumPackage> yumPackageIterator = removableYumPackages.iterator();
    while (yumPackageIterator.hasNext()) {
      hrefs.add(yumPackageIterator.next().getLocation().getHref());
    }
    assertThat(hrefs.contains("noarch/is24-dummyRpmForTesting-57034-2.noarch.rpm"), is(true));
    assertThat(hrefs.contains("noarch/is24-dummyRpmForTesting-57035-4.noarch.rpm"), is(true));
    assertThat(hrefs.contains("src/is24-dummyRpmForTesting-1-1.src.rpm"), is(true));
    assertThat(hrefs.contains("src/is24-dummyRpmForTesting-2-2.src.rpm"), is(true));


  }

  protected <T> T readJson(HttpResponse response, TypeReference<T> typeReference) throws IOException {
    return new ObjectMapper().readValue(response.getEntity().getContent(), typeReference);
  }

}
