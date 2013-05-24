package de.is24.infrastructure.gridfs.http.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.ReplicaSetStatus;
import com.mongodb.util.JSON;
import de.is24.util.monitoring.InApplicationMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_SINGLE_QUOTES;
import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;


@Controller
public class StatusController {
  private static final Logger LOGGER = LoggerFactory.getLogger(StatusController.class);

  public static final String OK_STATUS = "ok";
  public static final String NOT_RESPONDING_STATUS = "not responding";
  private final MongoTemplate mongoTemplate;
  private final Mongo mongo;

  private static final Set<String> EXPECTED_COLLECTION_NAMES = ImmutableSet.of(
    "fs.chunks",
    "fs.files",
    "system.indexes",
    "system.users",
    "yum.entries",
    "yum.repos");

  @Autowired
  public StatusController(final MongoTemplate mongoTemplate, Mongo mongo) {
    this.mongoTemplate = mongoTemplate;
    this.mongo = mongo;
  }

  @RequestMapping(value = "/status", method = GET, produces = { "application/json", "text/plain" })
  @ResponseBody
  public String getStatus(HttpServletResponse response,
                          @RequestParam(value = "pretty", required = false) boolean prettyJson) {
    final String rawJson = getStatusJson(response);
    return prettyJson ? prettyJson(rawJson) : rawJson;
  }

  private String prettyJson(final String rawJson) {
    try {
      final ObjectMapper mapper = new ObjectMapper().disableDefaultTyping()
        .configure(ALLOW_UNQUOTED_FIELD_NAMES, true)
        .configure(ALLOW_SINGLE_QUOTES, true);
      final Object o = mapper.readValue(rawJson, Object.class);
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(o);
    } catch (IOException ioEx) {
      LOGGER.debug("can't pretty json for rawJson {}\n{}", rawJson, ioEx);
      return rawJson;
    }
  }

  private String getStatusJson(HttpServletResponse response) {
    boolean isOK = false;

    final StringBuilder detailedInfo = new StringBuilder();
    try {
      isOK = checkPingTheNode();

      if (isOK) {
        final Set<String> collectionNames = mongoTemplate.getCollectionNames();
        isOK = collectionNames.containsAll(EXPECTED_COLLECTION_NAMES);

        appendInfoOnReplicaSet(detailedInfo);
        appendCollectionInfo(collectionNames, detailedInfo);
      }

    } catch (Exception e) {
      isOK = false;
    }
    if (!isOK) {
      response.setStatus(SC_SERVICE_UNAVAILABLE);
    }

    final StringBuilder content = new StringBuilder("{mainInfo:{mongoDBStatus: \"");
    content.append(isOK ? OK_STATUS : NOT_RESPONDING_STATUS);
    content.append("\"},detailedInfo: {");
    content.append(detailedInfo);
    content.append("}}");

    InApplicationMonitor.getInstance().incrementCounter(getClass().getName() + ".status");

    return content.toString();
  }

  private boolean checkPingTheNode() {
    final boolean ping = mongoTemplate.executeCommand(new BasicDBObject("ping", 1)).ok();
    return ping;
  }


  private void appendCollectionInfo(final Iterable<String> collectionNames, final StringBuilder content) {
    content.append(",collections: [");

    final Iterator<String> it = collectionNames.iterator();
    while (it.hasNext()) {
      final String collectionName = it.next();
      content.append("{name: \"");
      content.append(collectionName);
      content.append("\"");

      final CommandResult stats = getCollectionStats(collectionName);
      if (stats == null) {
        content.append(",info: \"unavialable\"");
      } else {
        content.append(",info: ");
        JSON.serialize(stats, content);
      }
      if (it.hasNext()) {
        content.append("},");
      } else {
        content.append("}");
      }
    }
    content.append("]");
  }

  private CommandResult getCollectionStats(final String collectionName) {
    final DBCollection dbCollection = mongoTemplate.getCollection(collectionName);
    if (dbCollection != null) {
      return dbCollection.getStats();
    }
    return null;
  }

  private void appendInfoOnReplicaSet(final StringBuilder content) {
    content.append("replicaSetInfo: ");

    ReplicaSetStatus replicaSetStatus = mongo.getReplicaSetStatus();
    if (replicaSetStatus == null) {
      content.append("\"unavialable\"");
    } else {
      content.append(replicaSetStatus.toString());
    }
  }
}
