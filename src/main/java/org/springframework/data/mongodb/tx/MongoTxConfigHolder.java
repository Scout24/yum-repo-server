package org.springframework.data.mongodb.tx;

public final class MongoTxConfigHolder {
  private static final ThreadLocal<MongoTxConfig> CONFIGS = new ThreadLocal<MongoTxConfig>();

  private MongoTxConfigHolder() {
  }

  public static MongoTxConfig get() {
    return CONFIGS.get();
  }

  public static void registerConfig(final MongoTxConfig mongoTxConfig) {
    CONFIGS.set(mongoTxConfig);
  }

  public static void resetConfig() {
    CONFIGS.remove();
  }

}
