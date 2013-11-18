package de.is24.infrastructure.gridfs.http.web.controller;

import de.is24.infrastructure.gridfs.http.domain.YumEntry;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackage;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageReducedView;
import de.is24.infrastructure.gridfs.http.metadata.YumEntriesRepository;
import de.is24.infrastructure.gridfs.http.monitoring.TimeMeasurement;
import de.is24.infrastructure.gridfs.http.rpm.version.YumPackageVersionComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_HTML_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;


@Controller
@RequestMapping("/maintenance")
@TimeMeasurement
public class MaintenanceController {
  private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceController.class);

  private YumPackageVersionComparator versionComparator = new YumPackageVersionComparator();

  private YumEntriesRepository yumEntriesRepository;

  @Autowired
  public MaintenanceController(YumEntriesRepository yumEntriesRepository) {
    this.yumEntriesRepository = yumEntriesRepository;
  }


  @RequestMapping(method = GET, produces = TEXT_HTML_VALUE)
  public ModelAndView getRepositoriesAsHtml(@RequestParam(value = "targetRepo", required = true) String targetRepo,
                                            @RequestParam(value = "sourceRepo", required = true) String sourceRepo) {
    Map<String, Map<String, YumPackage>> newestTargetPackages = findNewestPackages(yumEntriesRepository.findByRepo(
      targetRepo));
    List<YumEntry> sourceRepoEntries = yumEntriesRepository.findByRepo(sourceRepo);
    Set<YumPackageReducedView> obsoleteRPMs = determineObsoleteRPMs(newestTargetPackages, sourceRepoEntries);

    Map<String, Object> model = new HashMap<>();
    model.put("targetRepo", targetRepo);
    model.put("sourceRepo", sourceRepo);
    model.put("obsoleteRPMs", obsoleteRPMs);
    return new ModelAndView("obsoleteRPMs", model);
  }


  @RequestMapping(method = GET, produces = APPLICATION_JSON_VALUE, headers = "Accept=application/json")
  @ResponseBody
  public Set<YumPackageReducedView> getObsoletePRMsAsJson(
    @RequestParam(value = "targetRepo", required = true) String targetRepo,
    @RequestParam(value = "sourceRepo", required = true) String sourceRepo) {
    Map<String, Map<String, YumPackage>> newestTargetPackages = findNewestPackages(yumEntriesRepository.findByRepo(
      targetRepo));
    List<YumEntry> sourceRepoEntries = yumEntriesRepository.findByRepo(sourceRepo);
    Set<YumPackageReducedView> result = determineObsoleteRPMs(newestTargetPackages, sourceRepoEntries);
    return result;
  }

  private Set<YumPackageReducedView> determineObsoleteRPMs(
    Map<String, Map<String, YumPackage>> newestTargetPackagesByNameAndArch, List<YumEntry> sourceRepoEntries) {
    Set<YumPackageReducedView> result = new HashSet<YumPackageReducedView>();
    for (YumEntry entry : sourceRepoEntries) {
      YumPackage yumPackage = entry.getYumPackage();
      YumPackage newestPackageInTargetRepo = getMatchingYumPackageByNameAndArchIfAny(newestTargetPackagesByNameAndArch,
        yumPackage);
      LOGGER.info("comparing " + yumPackage + " to " + newestPackageInTargetRepo + " ...");
      if ((newestPackageInTargetRepo != null) &&
          (versionComparator.compare(newestPackageInTargetRepo.getVersion(), yumPackage.getVersion()) > 0)) {
        LOGGER.info(".. is obsolete");
        result.add(new YumPackageReducedView(yumPackage));
      }
    }
    return result;
  }

  private YumPackage getMatchingYumPackageByNameAndArchIfAny(Map<String, Map<String, YumPackage>> packagesByNameAndArch,
                                                             YumPackage yumPackage) {
    Map<String, YumPackage> rpmsByArch = packagesByNameAndArch.get(yumPackage.getName());
    if (rpmsByArch != null) {
      return rpmsByArch.get(yumPackage.getArch());
    }
    return null;
  }

  /**
  * determine newest RPMs by name and architecture
  * @param inputList list of yum entries in repo
  * @return a map of maps, first map key is rpm name, second maps key is arch
  */
  private Map<String, Map<String, YumPackage>> findNewestPackages(List<YumEntry> inputList) {
    Map<String, Map<String, YumPackage>> result = new HashMap<String, Map<String, YumPackage>>();
    for (YumEntry entry : inputList) {
      YumPackage yumPackage = entry.getYumPackage();

      Map<String, YumPackage> packageMap = result.get(yumPackage.getName());
      YumPackage packageForArchInMap = null;
      if (packageMap == null) {
        packageMap = new HashMap<String, YumPackage>();
        result.put(yumPackage.getName(), packageMap);
      } else {
        packageForArchInMap = packageMap.get(yumPackage.getArch());
      }
      if ((packageForArchInMap == null) ||
          (versionComparator.compare(yumPackage.getVersion(), packageForArchInMap.getVersion()) > 0)) {
        packageMap.put(yumPackage.getArch(), yumPackage);
      }
    }
    return result;
  }


  public void onError() {
  }


}
