package org.springframework.data.mongodb.tx;

import com.mongodb.FastestPingTimeReadPreference;
import com.mongodb.Mongo;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import de.is24.infrastructure.gridfs.http.mongo.util.LocalMongoFactory;
import de.is24.infrastructure.gridfs.http.mongo.util.MongoProcessHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import java.net.UnknownHostException;


@ComponentScan(basePackages = { "org.springframework.data.mongodb.tx" })
@Configuration
@EnableAspectJAutoProxy
public class MongoTxTextContext {
  @Bean
  public Mongo mongo() throws UnknownHostException {
    return new MongoTxProxy(new ServerAddress("localhost", mongoProcessHolder().getMongoPort()), mongoOptions());
  }

  private MongoClientOptions mongoOptions() {
    return new MongoClientOptions.Builder() //
      .autoConnectRetry(true)
      .socketKeepAlive(true)
      .readPreference(new FastestPingTimeReadPreference())
      .writeConcern(WriteConcern.JOURNALED)
      .connectionsPerHost(20)
      .socketTimeout(10 * 1000)
      .build();
  }

  @Bean
  public SimpleMongoDbFactory simpleMongoDbFactory() throws UnknownHostException {
    return new SimpleMongoDbFactory(mongo(), "rpm_db");
  }

  @Bean(destroyMethod = "stopMongo")
  public MongoProcessHolder mongoProcessHolder() {
    try {
      return LocalMongoFactory.createMongoProcess();
    } catch (Throwable throwable) {
      throw new RuntimeException(throwable);
    }
  }
}
