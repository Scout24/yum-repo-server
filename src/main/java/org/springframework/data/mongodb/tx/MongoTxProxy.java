package org.springframework.data.mongodb.tx;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCredential;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import java.net.UnknownHostException;
import java.util.List;


public class MongoTxProxy extends MongoClient {
  public MongoTxProxy() throws UnknownHostException {
  }

  public MongoTxProxy(String host) throws UnknownHostException {
    super(host);
  }

  public MongoTxProxy(String host, MongoClientOptions options) throws UnknownHostException {
    super(host, options);
  }

  public MongoTxProxy(String host, int port) throws UnknownHostException {
    super(host, port);
  }

  public MongoTxProxy(ServerAddress addr) {
    super(addr);
  }

  public MongoTxProxy(ServerAddress addr, List<MongoCredential> credentialsList) {
    super(addr, credentialsList);
  }

  public MongoTxProxy(ServerAddress addr, MongoClientOptions options) {
    super(addr, options);
  }

  public MongoTxProxy(ServerAddress addr, List<MongoCredential> credentialsList, MongoClientOptions options) {
    super(addr, credentialsList, options);
  }

  public MongoTxProxy(List<ServerAddress> seeds) {
    super(seeds);
  }

  public MongoTxProxy(List<ServerAddress> seeds, List<MongoCredential> credentialsList) {
    super(seeds, credentialsList);
  }

  public MongoTxProxy(List<ServerAddress> seeds, MongoClientOptions options) {
    super(seeds, options);
  }

  public MongoTxProxy(List<ServerAddress> seeds, List<MongoCredential> credentialsList, MongoClientOptions options) {
    super(seeds, credentialsList, options);
  }

  public MongoTxProxy(MongoClientURI uri) throws UnknownHostException {
    super(uri);
  }

  @Override
  public ReadPreference getReadPreference() {
    final MongoTxConfig mongoTxConfig = MongoTxConfigHolder.get();
    if (mongoTxConfig != null) {
      return mongoTxConfig.getReadPreference();
    }
    return super.getReadPreference();
  }

  @Override
  public WriteConcern getWriteConcern() {
    final MongoTxConfig mongoTxConfig = MongoTxConfigHolder.get();
    if ((mongoTxConfig != null) && (mongoTxConfig.getWriteConcern() != null)) {
      return mongoTxConfig.getWriteConcern();
    }
    return super.getWriteConcern();
  }
}
