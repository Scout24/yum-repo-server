package de.is24.infrastructure.gridfs.http.web.controller;

import de.is24.infrastructure.gridfs.http.domain.YumEntry;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackage;
import de.is24.infrastructure.gridfs.http.metadata.YumEntriesRepository;
import de.is24.infrastructure.gridfs.http.monitoring.TimeMeasurement;
import de.is24.infrastructure.gridfs.http.rpm.version.YumPackageVersionComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;


@Controller
@RequestMapping("/maintenance")
@TimeMeasurement
public class MaintenanceController {
  private YumPackageVersionComparator versionComparator = new YumPackageVersionComparator();

  private YumEntriesRepository yumEntriesRepository;

  @Autowired
  public MaintenanceController(YumEntriesRepository yumEntriesRepository) {
    this.yumEntriesRepository = yumEntriesRepository;
  }

  @RequestMapping(method = GET, produces = APPLICATION_JSON_VALUE, headers = "Accept=application/json")
  @ResponseBody
  public Set<YumPackage> getRepositoriesAsJson(@RequestParam(value = "targetRepo", required = true) String targetRepo,
                                               @RequestParam(value = "sourceRepo", required = true) String sourceRepo) {
    Map<String, YumPackage> newestTargetPackages = findNewestPackages(yumEntriesRepository.findByRepo(targetRepo));
    List<YumEntry> sourceRepoEntries = yumEntriesRepository.findByRepo(sourceRepo);
    Set<YumPackage> result = determineObsoleteRPMs(newestTargetPackages, sourceRepoEntries);
    return result;
  }

  private Set<YumPackage> determineObsoleteRPMs(Map<String, YumPackage> newestTargetPackages,
                                                List<YumEntry> sourceRepoEntries) {
    Set<YumPackage> result = new HashSet<YumPackage>();
    for (YumEntry entry : sourceRepoEntries) {
      YumPackage yumPackage = entry.getYumPackage();
      YumPackage newestPackageInTargetRepo = newestTargetPackages.get(yumPackage.getName());
      if ((newestPackageInTargetRepo != null) &&
          (versionComparator.compare(newestPackageInTargetRepo.getVersion(), yumPackage.getVersion()) > 0)) {
        result.add(yumPackage);
      }
    }
    return result;
  }

  private Map<String, YumPackage> findNewestPackages(List<YumEntry> inputList) {
    Map<String, YumPackage> result = new HashMap<String, YumPackage>();
    for (YumEntry entry : inputList) {
      YumPackage yumPackage = entry.getYumPackage();
      YumPackage packageInMap = result.get(yumPackage.getName());
      if (packageInMap == null) {
        result.put(yumPackage.getName(), yumPackage);
      } else {
        if (versionComparator.compare(yumPackage.getVersion(), packageInMap.getVersion()) > 0) {
          result.put(yumPackage.getName(), yumPackage);
        }
      }
    }
    return result;
  }


  public void onError() {
  }


}
