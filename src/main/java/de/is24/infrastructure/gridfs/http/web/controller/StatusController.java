package de.is24.infrastructure.gridfs.http.web.controller;

import com.mongodb.BasicDBObject;
import de.is24.util.monitoring.InApplicationMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import javax.servlet.http.HttpServletResponse;
import static java.lang.String.format;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;


@Controller
public class StatusController {
  private static final Logger LOGGER = LoggerFactory.getLogger(StatusController.class);

  public static final String OK_STATUS = "ok";
  public static final String NOT_RESPONDING_STATUS = "not responding";
  private final MongoTemplate mongo;

  @Autowired
  public StatusController(final MongoTemplate mongo) {
    this.mongo = mongo;
  }

  @RequestMapping(value = "/status", method = GET)
  @ResponseBody
  public String getStatus(HttpServletResponse response) {
    boolean isOK = false;
    try {
      isOK = mongo.executeCommand(new BasicDBObject("ping", 1)).ok();
    } catch (Exception ex) {
      LOGGER.warn("could not execute ping command", ex);
      isOK = false;
    }
    if (!isOK) {
      response.setStatus(SC_SERVICE_UNAVAILABLE);
    }

    InApplicationMonitor.getInstance().incrementCounter(getClass().getName() + ".status");
    return format("{mongoDBStatus: '%s'}", isOK ? OK_STATUS : NOT_RESPONDING_STATUS);
  }
}
