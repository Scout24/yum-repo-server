package de.is24.infrastructure.gridfs.http;

import de.is24.util.monitoring.CorePlugin;
import de.is24.util.monitoring.InApplicationMonitor;
import de.is24.util.monitoring.jmx.SimpleJmxAppmon4jNamingStrategy;
import de.is24.util.monitoring.keyhandler.DefaultKeyEscaper;
import de.is24.util.monitoring.keyhandler.KeyHandler;
import de.is24.util.monitoring.state2graphite.StateValuesToGraphite;
import de.is24.util.monitoring.statsd.StatsdPlugin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;

import java.net.SocketException;
import java.net.UnknownHostException;

@Configuration
@EnableMBeanExport
public class MonitoringConfig {

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

  @Bean(destroyMethod = "destroy")
  public CorePlugin corePlugin() {
    CorePlugin corePlugin = new CorePlugin(new SimpleJmxAppmon4jNamingStrategy("yumRepoServer"), keyHandler());
    corePlugin.readJMXExporterPatternFromDir("/etc/appmon4j-jmxexport/yumRepoServer");
    return corePlugin;
  }

  @Bean
  public KeyHandler keyHandler() {
    return new DefaultKeyEscaper();
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
}
