package org.springframework.data.mongodb.tx;

import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;


public class MongoTxConfig {
  private final WriteConcern writeConcern;
  private final ReadPreference readPreference;

  public MongoTxConfig(WriteConcern writeConcern, ReadPreference readPreference) {
    this.writeConcern = writeConcern;
    this.readPreference = readPreference;
  }

  public WriteConcern getWriteConcern() {
    return writeConcern;
  }

  public ReadPreference getReadPreference() {
    return readPreference;
  }
}
