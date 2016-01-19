package de.is24.infrastructure.gridfs.http.mongo;

import com.mongodb.FastestPingTimeReadPreference;
import com.mongodb.Mongo;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.gridfs.GridFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.mongodb.tx.MongoTxProxy;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.mongodb.MongoCredential.createCredential;
import static com.mongodb.WriteConcern.ACKNOWLEDGED;
import static com.mongodb.WriteConcern.W2;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.regex.Pattern.compile;

@Configuration
@EnableMongoRepositories("de.is24.infrastructure.gridfs.http.metadata")
public class MongoConfig extends AbstractMongoConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(MongoConfig.class);
  private static final int CLUSTER_SIZE = 3;
  private static final String SEPARATOR = ",";
  private static final Pattern SEPARATOR_PATTERN = compile(SEPARATOR);

  @Value("${mongodb.serverlist}")
  String mongoDBServerList;

  @Value("${mongodb.db.name:rpm_db}")
  String mongoDBName;

  @Value("${mongodb.db.user:@null}")
  String mongoDBUsername;

  @Value("${mongodb.db.pass:@null}")
  String mongoDBPassword;

  @Value("${mongodb.port:27017}")
  String mongoDBPort;

  @Value("${mongodb.socket.timeout:60}")
  int mongoDBSocketTimeoutInSec;

  @Bean
  @Override
  public Mongo mongo() throws UnknownHostException {
    return new MongoTxProxy(getReplicatSet(), getCredentials(), mongoOptions());
  }

  @Bean
  @Override
  public MongoTemplate mongoTemplate() throws Exception {
    int tries = 0;
    while (tries < 3) {
      try {
        return new MongoTemplate(mongoDbFactory(), mappingMongoConverter());
      } catch (MongoException e) {
        if (e.getMessage().contains("can't find a master")) {
          tries++;
          LOGGER.warn("when creatig MongoTemplate: could not find a master, will retry in 10 seconds");

          // switching mongo primary takes ~10 seconds
          Thread.sleep(10000);
        } else {
          throw e;
        }
      }
    }
    throw new MongoException("could not find a master after three tries");
  }


  private MongoClientOptions mongoOptions() {
    return new MongoClientOptions.Builder() //
        .socketKeepAlive(true)
        .readPreference(new FastestPingTimeReadPreference())
        .writeConcern(getWriteConcern())
        .connectionsPerHost(100)
        .threadsAllowedToBlockForConnectionMultiplier(10)
        .socketTimeout(mongoDBSocketTimeoutInSec * 1000)
        .build();
  }

  @Bean
  public GridFS gridFs() throws Exception {
    return new GridFS(mongoDbFactory().getDb());
  }

  @Bean
  public GridFsTemplate gridFsTemplate() throws Exception {
    return new GridFsTemplate(mongoDbFactory(), mappingMongoConverter());
  }

  private WriteConcern getWriteConcern() {
    if (mongoDBServerList.contains(SEPARATOR)) {
      return W2;
    }

    return ACKNOWLEDGED;
  }

  private List<ServerAddress> getReplicatSet() throws UnknownHostException {
    List<ServerAddress> hosts = new ArrayList<>(CLUSTER_SIZE);


    for (String curServer : SEPARATOR_PATTERN.split(mongoDBServerList)) {
      ServerAddress serverAddress;
      serverAddress = new ServerAddress(curServer, Integer.parseInt(mongoDBPort));
      hosts.add(serverAddress);
    }

    LOGGER.info("Start with " + hosts.toString());
    return hosts;
  }

  @Override
  protected String getDatabaseName() {
    return mongoDBName;
  }

  protected List<MongoCredential> getCredentials() {
    if (mongoDBUsername != null) {
      return singletonList(createCredential(mongoDBUsername, mongoDBName, mongoDBPassword.toCharArray()));
    }

    return emptyList();
  }
}
