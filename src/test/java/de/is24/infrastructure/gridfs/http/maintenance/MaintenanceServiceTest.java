package de.is24.infrastructure.gridfs.http.maintenance;

import de.is24.infrastructure.gridfs.http.domain.YumEntry;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackage;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageReducedView;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageVersion;
import de.is24.infrastructure.gridfs.http.gridfs.GridFsService;
import de.is24.infrastructure.gridfs.http.metadata.YumEntriesRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.scheduling.TaskScheduler;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class MaintenanceServiceTest {
  @Mock
  private TaskScheduler taskScheduler;
  @Mock
  private YumEntriesRepository yumEntriesRepository;
  @Mock
  private GridFsService gridFsService;

  @InjectMocks
  private MaintenanceService maintenanceService;


  private List<YumEntry> targetList;
  private List<YumEntry> sourceList;
  private YumEntry shouldBeObsolete1;
  private YumEntry shouldBeObsolete2;
  private YumEntry shouldBePropagatable1;
  private YumEntry shouldBePropagatable2;
  private YumEntry shouldBeObsoleteSrc1;

  @Before
  public void setup() {
    targetList = new ArrayList<YumEntry>();
    targetList.add(createYumEntry("target", "dummy", "noarch", 3, 2));
    targetList.add(createYumEntry("target", "dummy", "noarch", 2, 1));
    targetList.add(createYumEntry("target", "dummy", "src", 11, 1));

    shouldBeObsolete1 = createYumEntry("source", "dummy", "noarch", 1, 1);
    shouldBeObsolete2 = createYumEntry("source", "dummy", "noarch", 2, 2);
    shouldBePropagatable1 = createYumEntry("source", "dummy", "noarch", 3, 3);
    shouldBePropagatable2 = createYumEntry("source", "dummy", "noarch", 4, 4);
    shouldBeObsoleteSrc1 = createYumEntry("target", "dummy", "src", 10, 1);


    sourceList = new ArrayList<YumEntry>();
    sourceList.add(shouldBeObsolete1);
    sourceList.add(shouldBeObsolete2);
    sourceList.add(shouldBePropagatable1);
    sourceList.add(shouldBePropagatable2);
    sourceList.add(shouldBeObsoleteSrc1);
  }

  private YumEntry createYumEntry(String repo, String name, String arch, int version, int release) {
    YumPackage yumPackage = new YumPackage();
    yumPackage.setName(name);
    yumPackage.setArch(arch);

    YumPackageVersion packageVersion = new YumPackageVersion();
    packageVersion.setVer(Integer.toString(version));
    packageVersion.setRel(Integer.toString(release));
    yumPackage.setVersion(packageVersion);
    return new YumEntry(null, repo, yumPackage);
  }

  @Test
  public void findObsoleteRPMs() throws Exception {
    when(yumEntriesRepository.findByRepo("target")).thenReturn(targetList);
    when(yumEntriesRepository.findByRepo("source")).thenReturn(sourceList);

    Set<YumPackageReducedView> obsoleteRPMs = maintenanceService.getObsoleteRPMs("target", "source");

    assertThat(obsoleteRPMs.size(), is(3));
    assertThat(obsoleteRPMs.contains(new YumPackageReducedView(shouldBeObsolete1.getYumPackage())), is(true));
    assertThat(obsoleteRPMs.contains(new YumPackageReducedView(shouldBeObsolete2.getYumPackage())), is(true));
    assertThat(obsoleteRPMs.contains(new YumPackageReducedView(shouldBeObsoleteSrc1.getYumPackage())), is(true));

  }

  @Test
  public void findPropagatableRPMs() throws Exception {
    when(yumEntriesRepository.findByRepo("target")).thenReturn(targetList);
    when(yumEntriesRepository.findByRepo("source")).thenReturn(sourceList);

    Set<YumPackageReducedView> obsoleteRPMs = maintenanceService.getPropagatableRPMs("target", "source");

    assertThat(obsoleteRPMs.size(), is(2));
    assertThat(obsoleteRPMs.contains(new YumPackageReducedView(shouldBePropagatable1.getYumPackage())), is(true));
    assertThat(obsoleteRPMs.contains(new YumPackageReducedView(shouldBePropagatable2.getYumPackage())), is(true));
  }


}
