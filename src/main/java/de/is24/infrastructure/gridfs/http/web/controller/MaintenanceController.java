package de.is24.infrastructure.gridfs.http.web.controller;

import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageReducedView;
import de.is24.infrastructure.gridfs.http.maintenance.MaintenanceService;
import de.is24.infrastructure.gridfs.http.monitoring.TimeMeasurement;
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


  /* for AOP autoproxying */
  protected MaintenanceController() {
  }


  @Autowired
  public MaintenanceController(MaintenanceService maintenanceService) {
    this.maintenanceService = maintenanceService;
  }


  @RequestMapping(method = GET, produces = TEXT_HTML_VALUE)
  @TimeMeasurement
  public ModelAndView showMaintenaceOptions() {
    Map<String, Object> model = new HashMap<>();
    setViewName(model);
    return new ModelAndView("maintenanceOptions", model);
  }

  @RequestMapping(value = "/obsolete", method = GET, produces = TEXT_HTML_VALUE)
  @TimeMeasurement
  public ModelAndView getObsoleteRPMsAsHtml(@RequestParam(value = "targetRepo", required = true) String targetRepo,
                                            @RequestParam(value = "sourceRepo", required = true) String sourceRepo) {
    Map<String, Object> model = new HashMap<>();
    setViewName(model);
    model.put("targetRepo", targetRepo);
    model.put("sourceRepo", sourceRepo);
    model.put("obsoleteRPMs", maintenanceService.getObsoleteRPMs(targetRepo, sourceRepo));
    return new ModelAndView("obsoleteRPMs", model);
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
    Map<String, Object> model = new HashMap<>();
    setViewName(model);
    model.put("targetRepo", targetRepo);
    model.put("sourceRepo", sourceRepo);
    model.put("propagatableRPMs", maintenanceService.getPropagatableRPMs(targetRepo, sourceRepo));
    return new ModelAndView("propagatableRPMs", model);
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


  public void onError() {
  }

  private void setViewName(Map<String, Object> model) {
    model.put("viewName", "maintenance");
  }


}
