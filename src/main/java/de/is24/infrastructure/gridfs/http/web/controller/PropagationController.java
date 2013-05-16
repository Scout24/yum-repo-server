package de.is24.infrastructure.gridfs.http.web.controller;

import de.is24.infrastructure.gridfs.http.gridfs.GridFsService;
import de.is24.infrastructure.gridfs.http.monitoring.TimeMeasurement;
import de.is24.util.monitoring.InApplicationMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import javax.servlet.http.HttpServletResponse;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.web.bind.annotation.RequestMethod.POST;


@Controller
@TimeMeasurement
public class PropagationController {
  private final GridFsService gridFs;
  private static final Logger LOG = LoggerFactory.getLogger(PropagationController.class);

  // just for CGLIB
  PropagationController() {
    this.gridFs = null;
  }

  @Autowired
  public PropagationController(GridFsService gridFs) {
    this.gridFs = gridFs;
  }

  @RequestMapping(value = "/propagation", method = POST)
  @ResponseStatus(CREATED)
  public void propgateRpm(@RequestParam("source") String sourcePath,
                          @RequestParam("destination") String destinationRepo, HttpServletResponse response) {
    String destinationPath = gridFs.propagateRpm(sourcePath, destinationRepo);

    LOG.info("Moved {} to repository {}", sourcePath, destinationRepo);
    response.addHeader("Location", FileController.PREFIX + "/" + destinationPath);
    InApplicationMonitor.getInstance().incrementCounter(getClass().getName() + ".propagate.rpm");
  }

  @RequestMapping(value = "/repo-propagation", method = POST)
  @ResponseStatus(CREATED)
  public void propgateRepository(@RequestParam("source") String sourceRepo,
                                 @RequestParam("destination") String destinationRepo, HttpServletResponse response) {
    gridFs.propagateRepository(sourceRepo, destinationRepo);

    LOG.info("Moved repository content of {} to repository {}", sourceRepo, destinationRepo);
    response.addHeader("Location", FileController.PREFIX + "/" + destinationRepo);
    InApplicationMonitor.getInstance().incrementCounter(getClass().getName() + ".propagate.repo");
  }
}
