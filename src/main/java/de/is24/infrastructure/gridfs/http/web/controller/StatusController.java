package de.is24.infrastructure.gridfs.http.web.controller;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.mongodb.BasicDBObject;
import com.mongodb.Mongo;
import com.mongodb.ReplicationSetStatusBridge;
import com.mongodb.ServerAddress;
import de.is24.util.monitoring.InApplicationMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import static java.lang.String.format;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;


@Controller
public class StatusController {
  private static final Logger LOGGER = LoggerFactory.getLogger(StatusController.class);

  public static final String OK_STATUS = "ok";
  public static final String NOT_RESPONDING_STATUS = "not responding";
  private final MongoTemplate mongoTemplate;
  private final Mongo mongo;

  @Autowired
  public StatusController(final MongoTemplate mongoTemplate, Mongo mongo) {
    this.mongoTemplate = mongoTemplate;
    this.mongo = mongo;
  }

  @RequestMapping(value = "/status", method = GET)
  @ResponseBody
  public String getStatus(HttpServletResponse response) {
    boolean isOK = checkCompletenessOfSetOfCollections();

    if (isOK) {
      isOK = checkPingTheNode();
    }

    if (!isOK) {
      response.setStatus(SC_SERVICE_UNAVAILABLE);
    }

    InApplicationMonitor.getInstance().incrementCounter(getClass().getName() + ".status");

    String status = format("{mongoDBStatus: '%s'}", isOK ? OK_STATUS : NOT_RESPONDING_STATUS);
    return status + "<p/>" + getInfoOnCollections() + "<p/>" + "<pre>" + getInfoOnReplicaSet() + "</pre>";
  }

  private boolean checkPingTheNode() {
    boolean isOK;
    try {
      isOK = mongoTemplate.executeCommand(new BasicDBObject("ping", 1)).ok();
    } catch (Exception ex) {
      LOGGER.warn("could not execute ping command", ex);
      isOK = false;
    }
    return isOK;
  }

  private boolean checkCompletenessOfSetOfCollections() {
    Set<String> expectedSetOfCollections = Sets.newHashSet(
      "fs.chunks", //
      "fs.files", //
      "system.indexes", //
      "system.users", //
      "yum.entries", //
      "yum.repos");

    boolean isOk = mongoTemplate.getCollectionNames().containsAll(expectedSetOfCollections);

    if (!isOk) {
      LOGGER.warn("collections not complete");
    }
    return isOk;
  }

  private String getInfoOnCollections() {
    Set<String> collectionNames = mongoTemplate.getCollectionNames();
    return Joiner.on("<br/>").join(collectionNames);
  }

  private String getInfoOnReplicaSet() {
    ReplicationSetStatusBridge replicationSetStatusBridge = new ReplicationSetStatusBridge(mongo.getReplicaSetStatus());
    List<String> info = new ArrayList<>();
    for (ServerAddress server : mongo.getServerAddressList()) {
      info.add(server + "\t: " + replicationSetStatusBridge.getState(server));
    }

    return Joiner.on("<br/>").join(info);
  }
}
