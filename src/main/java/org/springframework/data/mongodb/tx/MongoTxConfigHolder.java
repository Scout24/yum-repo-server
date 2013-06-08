package org.springframework.data.mongodb.tx;

public class MongoTxConfigHolder {
  private static final ThreadLocal<MongoTxConfig> configs = new ThreadLocal<MongoTxConfig>();

  public static MongoTxConfig get() {
    return configs.get();
  }

  public static void registerConfig(final MongoTxConfig mongoTxConfig) {
    configs.set(mongoTxConfig);
  }

  public static void resetConfig() {
    configs.remove();
  }

}
