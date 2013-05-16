package de.is24.infrastructure.gridfs.http;

import com.mongodb.FastestPingTimeReadPreference;
import com.mongodb.Mongo;
import com.mongodb.MongoOptions;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.gridfs.GridFS;
import de.is24.util.monitoring.InApplicationMonitor;
import de.is24.util.monitoring.state2graphite.StateValuesToGraphite;
import de.is24.util.monitoring.statsd.StatsdPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.env.Environment;
import org.springframework.data.authentication.UserCredentials;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import static com.mongodb.WriteConcern.NORMAL;
import static com.mongodb.WriteConcern.REPLICAS_SAFE;


@ComponentScan(basePackages = { "de.is24" })
@Configuration
@EnableAspectJAutoProxy
@EnableMBeanExport
@EnableMongoRepositories
@EnableScheduling
@Import(PropertyConfig.class)
@ImportResource("classpath:/security-context.xml")
public class AppConfig extends AbstractMongoConfiguration {
  private static final Logger LOGGER = LoggerFactory.getLogger(AppConfig.class);

  private static final int CLUSTER_SIZE = 3;
  public static final String SEPARATOR = ",";
  private static final Pattern SEPARATOR_PATTERN = Pattern.compile(SEPARATOR);

  @Autowired
  Environment env;

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

  @Value("${graphite.host:@null}")
  String graphiteHost;

  @Value("${graphite.port:2003}")
  int graphitePort;

  @Value("${statsd.host:@null}")
  String statsdHost;

  @Value("${statsd.port:8125}")
  int statsdPort;

  @Value("${typ:@null}")
  String typ;

  @Value("${scheduler.poolSize:10}")
  int schedulerPoolSize;

  @Bean
  @Override
  public Mongo mongo() throws Exception {
    return new Mongo(getReplicatSet(), mongoOptions());
  }

  private MongoOptions mongoOptions() throws Exception {
    MongoOptions mongoOptions = new MongoOptions();
    mongoOptions.setAutoConnectRetry(true);
    mongoOptions.setSocketKeepAlive(true);
    mongoOptions.setReadPreference(new FastestPingTimeReadPreference());
    mongoOptions.setConnectionsPerHost(20);
    mongoOptions.setSocketTimeout(10 * 1000);
    return mongoOptions;
  }

  @Bean
  public GridFS gridFs() throws Exception {
    return new GridFS(mongoDbFactory().getDb());
  }

  @Bean
  public GridFsTemplate gridFsTemplate() throws Exception {
    return new GridFsTemplate(mongoDbFactory(), mappingMongoConverter());
  }

  @Override
  public SimpleMongoDbFactory mongoDbFactory() throws Exception {
    SimpleMongoDbFactory dbFactory = super.mongoDbFactory();
    dbFactory.setWriteConcern(getWriteConcern());
    return dbFactory;
  }

  private WriteConcern getWriteConcern() {
    if (mongoDBServerList.contains(SEPARATOR)) {
      return REPLICAS_SAFE;
    }

    return NORMAL;
  }

  @Bean
  public StatsdPlugin statsdPlugin() throws Exception {
    if (statsdHost != null) {
      StatsdPlugin statsdPlugin = new StatsdPlugin(statsdHost, statsdPort, typ);
      statsdPlugin.register();
      return statsdPlugin;
    }

    return null;
  }

  @Bean(destroyMethod = "shutdown")
  public StateValuesToGraphite stateValuesToGraphite() {
    if (graphiteHost != null) {
      return new StateValuesToGraphite(graphiteHost, graphitePort, typ);
    }

    return null;
  }

  private List<ServerAddress> getReplicatSet() throws UnknownHostException {
    List<ServerAddress> hosts = new ArrayList<ServerAddress>(CLUSTER_SIZE);


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

  @Override
  protected UserCredentials getUserCredentials() {
    if (mongoDBUsername != null) {
      return new UserCredentials(mongoDBUsername, mongoDBPassword);
    }

    return null;
  }

  @Bean
  public StandardServletMultipartResolver multipartResolver() {
    return new StandardServletMultipartResolver();
  }

  @Bean
  public ThreadPoolTaskScheduler taskScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(schedulerPoolSize);
    scheduler.setThreadGroupName("metadata.scheduler");
    setupMonitorForQueueSize(scheduler);
    return scheduler;
  }

  private void setupMonitorForQueueSize(final ThreadPoolTaskScheduler scheduler) {
    InApplicationMonitor.getInstance().registerStateValue(new QueueSizeValueProvider(scheduler));
  }

  @Bean
  public InApplicationMonitor inApplicationMonitor() {
    return InApplicationMonitor.getInstance();
  }
}
