package de.is24.infrastructure.gridfs.http;

import com.mongodb.FastestPingTimeReadPreference;
import com.mongodb.Mongo;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.gridfs.GridFS;
import de.is24.infrastructure.gridfs.http.security.MethodSecurityConfig;
import de.is24.infrastructure.gridfs.http.security.UserAuthorities;
import de.is24.infrastructure.gridfs.http.security.WebSecurityConfig;
import de.is24.util.monitoring.CorePlugin;
import de.is24.util.monitoring.InApplicationMonitor;
import de.is24.util.monitoring.jmx.SimpleJmxAppmon4jNamingStrategy;
import de.is24.util.monitoring.keyhandler.DefaultKeyEscaper;
import de.is24.util.monitoring.keyhandler.KeyHandler;
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
import org.springframework.core.env.Environment;
import org.springframework.data.authentication.UserCredentials;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.mongodb.tx.MongoTxProxy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.concurrent.DelegatingSecurityContextScheduledExecutorService;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.regex.Pattern;
import static com.mongodb.WriteConcern.NORMAL;
import static com.mongodb.WriteConcern.REPLICAS_SAFE;


@ComponentScan(basePackages = { "de.is24", "org.springframework.data.mongodb.tx" })
@Configuration
@EnableAspectJAutoProxy
@EnableMBeanExport
@EnableMongoRepositories
@EnableScheduling
@Import({ PropertyConfig.class, WebSecurityConfig.class, MethodSecurityConfig.class })
public class AppConfig extends AbstractMongoConfiguration {
  private static final Logger LOGGER = LoggerFactory.getLogger(AppConfig.class);

  private static final int CLUSTER_SIZE = 3;
  public static final String SEPARATOR = ",";
  private static final Pattern SEPARATOR_PATTERN = Pattern.compile(SEPARATOR);
  public static final String METADATA_SCHEDULER = "metadata.scheduler";

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

  @Value("${mongodb.socket.timeout:60}")
  int mongoDBSocketTimeoutInSec;

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
  public Mongo mongo() throws UnknownHostException {
    return new MongoTxProxy(getReplicatSet(), mongoOptions());
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
      return REPLICAS_SAFE;
    }

    return NORMAL;
  }


  @Bean
  public KeyHandler keyHandler() {
    return new DefaultKeyEscaper();
  }

  @Bean(destroyMethod = "destroy")
  public CorePlugin corePlugin() {
    CorePlugin corePlugin = new CorePlugin(new SimpleJmxAppmon4jNamingStrategy("yumRepoServer"), keyHandler());
    corePlugin.readJMXExporterPatternFromDir("/etc/appmon4j-jmxexport/yumRepoServer");
    return corePlugin;
  }

  @Bean(destroyMethod = "removeAllPlugins")
  public InApplicationMonitor inApplicationMonitor() {
    return InApplicationMonitor.initInstance(corePlugin(), keyHandler());
  }


  @Bean(initMethod = "register", destroyMethod = "afterRemovalNotification")
  public StatsdPlugin appmon4jStatsdPlugin() {
    if (statsdHost != null) {
      try {
        return new StatsdPlugin(statsdHost, statsdPort, typ);
      } catch (UnknownHostException e) {
        return null;
      } catch (SocketException e) {
        return null;
      }
    }

    return null;
  }

  @Bean(destroyMethod = "shutdown")
  public StateValuesToGraphite appmon4jStateValuesExport() {
    if (graphiteHost != null) {
      return new StateValuesToGraphite(graphiteHost, graphitePort, typ);
    }

    return null;
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
  public ScheduledExecutorService scheduledExecutorService() {
    ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(schedulerPoolSize);

    setupMonitorForQueueSize(scheduledThreadPoolExecutor);

    SecurityContext context = SecurityContextHolder.getContext();
    context.setAuthentication(
      new PreAuthenticatedAuthenticationToken(METADATA_SCHEDULER + ".user",
        "no credentials",
        UserAuthorities.USER_AUTHORITIES));

    ScheduledExecutorService scheduledExecutorService = new DelegatingSecurityContextScheduledExecutorService(
      scheduledThreadPoolExecutor,
      context);
    return scheduledExecutorService;
  }

  private void setupMonitorForQueueSize(final ScheduledThreadPoolExecutor scheduler) {
    inApplicationMonitor().registerStateValue(new QueueSizeValueProvider(scheduler, METADATA_SCHEDULER));
  }

}
