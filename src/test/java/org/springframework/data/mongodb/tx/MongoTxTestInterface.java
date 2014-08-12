package org.springframework.data.mongodb.tx;

import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;


public interface MongoTxTestInterface {
  void txMethodNativeMongo(WriteConcern expectedWriteConcern, ReadPreference expectedReadPref);

  void txMethodNativeMongoWithDefaults(WriteConcern expectedWriteConcern,
                                       ReadPreference expectedReadPref);

  void txMethodNativeMongoWithUnknownWriteConcern(WriteConcern expectedWriteConcern,
                                                  ReadPreference expectedReadPref);

  void txMethodMongoFactory(WriteConcern expectedWriteConcern, ReadPreference expectedReadPref);

  void nonTxMethodNativeMongo(final WriteConcern expectedWriteConcern, final ReadPreference expectedReadPref);

  void nonTxMethodMongoFactory(final WriteConcern expectedWriteConcern, final ReadPreference expectedReadPref);
}
