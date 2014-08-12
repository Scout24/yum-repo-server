package de.is24.infrastructure.gridfs.http.web.controller;

import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageReducedView;
import de.is24.infrastructure.gridfs.http.maintenance.MaintenanceService;
import de.is24.infrastructure.gridfs.http.repos.RepoService;
import de.is24.infrastructure.gridfs.http.storage.FileStorageItem;
import de.is24.util.monitoring.spring.TimeMeasurement;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_HTML_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;


@Controller
@RequestMapping("/maintenance")
@TimeMeasurement
public class MaintenanceController {
  private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceController.class);

  private MaintenanceService maintenanceService;
  private RepoService repoService;


  /* for AOP autoproxying */
  protected MaintenanceController() {
  }


  @Autowired
  public MaintenanceController(MaintenanceService maintenanceService, RepoService repoService) {
    this.maintenanceService = maintenanceService;
    this.repoService = repoService;
  }


  @RequestMapping(method = GET, produces = TEXT_HTML_VALUE)
  @TimeMeasurement
  public ModelAndView showMaintenaceOptions() {
    Map<String, Object> model = prepareModel();
    return new ModelAndView("maintenanceOptions", model);
  }

  private Map<String, Object> prepareModel() {
    Map<String, Object> model = new HashMap<>();
    model.put("viewName", "maintenance");
    model.put("error", Boolean.FALSE);

    model.put("propagatableSourceRepoInvalid", Boolean.FALSE);
    model.put("propagatableTargetRepoInvalid", Boolean.FALSE);
    model.put("obsoleteSourceRepoInvalid", Boolean.FALSE);
    model.put("obsoleteTargetRepoInvalid", Boolean.FALSE);
    return model;
  }

  @RequestMapping(value = "/obsolete", method = GET, produces = TEXT_HTML_VALUE)
  @TimeMeasurement
  public ModelAndView getObsoleteRPMsAsHtml(@RequestParam(value = "targetRepo", required = true) String targetRepo,
                                            @RequestParam(value = "sourceRepo", required = true) String sourceRepo) {
    Map<String, Object> model = prepareModel();

    boolean reposAreVaid = validateRepos(model, "obsolete", sourceRepo, targetRepo);
    if (reposAreVaid) {
      model.put("obsoleteRPMs", maintenanceService.getObsoleteRPMs(targetRepo, sourceRepo));
      model.put("targetRepo", targetRepo);
      model.put("sourceRepo", sourceRepo);
      return new ModelAndView("obsoleteRPMs", model);
    } else {
      model.put("error", Boolean.TRUE);
      model.put("errorMessage", "At least one of the given repos does not exist or is not a static repository");
      model.put("obsoleteTargetRepo", targetRepo);
      model.put("obsoleteSourceRepo", sourceRepo);
      return new ModelAndView("maintenanceOptions", model);
    }
  }


  @RequestMapping(
    value = "/obsolete", method = GET, produces = APPLICATION_JSON_VALUE, headers = "Accept=application/json"
  )
  @ResponseBody
  @TimeMeasurement
  public Set<YumPackageReducedView> getObsoletePRMsAsJson(
    @RequestParam(value = "targetRepo", required = true) String targetRepo,
    @RequestParam(value = "sourceRepo", required = true) String sourceRepo) {
    return maintenanceService.getObsoleteRPMs(targetRepo, sourceRepo);
  }

  @RequestMapping(value = "/obsolete", method = DELETE)
  @ResponseStatus(ACCEPTED)
  public void deleteObsoletePRMs(@RequestParam(value = "targetRepo", required = true) String targetRepo,
                                 @RequestParam(value = "sourceRepo", required = true) String sourceRepo) {
    maintenanceService.triggerDeletionOfObsoleteRPMs("Request by " +
      (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal(), targetRepo, sourceRepo);
  }


  @RequestMapping(value = "/propagatable", method = GET, produces = TEXT_HTML_VALUE)
  @TimeMeasurement
  public ModelAndView getPropagatableRPMsAsHtml(
    @RequestParam(value = "targetRepo", required = true) String targetRepo,
    @RequestParam(value = "sourceRepo", required = true) String sourceRepo) {
    Map<String, Object> model = prepareModel();

    boolean reposAreVaid = validateRepos(model, "propagatable", sourceRepo, targetRepo);
    if (reposAreVaid) {
      model.put("targetRepo", targetRepo);
      model.put("sourceRepo", sourceRepo);
      model.put("propagatableRPMs", maintenanceService.getPropagatableRPMs(targetRepo, sourceRepo));
      return new ModelAndView("propagatableRPMs", model);
    } else {
      model.put("error", Boolean.TRUE);
      model.put("errorMessage", "At least one of the given repos does not exist or is not a static repository");
      model.put("propagatableTargetRepo", targetRepo);
      model.put("propagatableSourceRepo", sourceRepo);
      return new ModelAndView("maintenanceOptions", model);
    }


  }

  @RequestMapping(
    value = "/propagatable", method = GET, produces = APPLICATION_JSON_VALUE, headers = "Accept=application/json"
  )
  @ResponseBody
  @TimeMeasurement
  public Set<YumPackageReducedView> getPropagatablePRMsAsJson(
    @RequestParam(value = "targetRepo", required = true) String targetRepo,
    @RequestParam(value = "sourceRepo", required = true) String sourceRepo) {
    return maintenanceService.getPropagatableRPMs(targetRepo, sourceRepo);
  }

  @RequestMapping(
    value = "/consistency/entries", method = GET, produces = APPLICATION_JSON_VALUE,
    headers = "Accept=application/json"
  )
  @ResponseBody
  @TimeMeasurement
  public Map<ObjectId, YumPackageReducedView> getYumEntriesWithoutGridFsFile() {
    return maintenanceService.getYumEntriesWithoutAssociatedFiles();
  }

  @RequestMapping(
      value = "/consistency/entries", method = DELETE, produces = APPLICATION_JSON_VALUE,
      headers = "Accept=application/json"
  )
  @ResponseBody
  @TimeMeasurement
  public Map<ObjectId, YumPackageReducedView> deleteYumEntriesWithoutGridFsFile() {
    return maintenanceService.deleteYumEntriesWithoutAssociatedFiles();
  }

  @RequestMapping(
    value = "/consistency/files", method = GET, produces = APPLICATION_JSON_VALUE, headers = "Accept=application/json"
  )
  @ResponseBody
  @TimeMeasurement
  public Set<FileStorageItem> getFilesWithoutYumEntries() {
    return maintenanceService.getFilesWithoutYumEntry();
  }

  private boolean validateRepos(Map<String, Object> model, String prefix, String sourceRepo,
                                String targetRepo) {
    boolean okay = true;
    if (!repoService.staticRepoExists(sourceRepo)) {
      okay = false;
      model.put(prefix + "SourceRepoInvalid", Boolean.TRUE);
    }
    if (!repoService.staticRepoExists(targetRepo)) {
      okay = false;
      model.put(prefix + "TargetRepoInvalid", Boolean.TRUE);
    }
    return okay;
  }


  public void onError() {
  }


}
