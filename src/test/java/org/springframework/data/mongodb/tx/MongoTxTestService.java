package org.springframework.data.mongodb.tx;

import com.mongodb.Mongo;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.stereotype.Service;


@Service
public class MongoTxTestService extends MongoTxTestComponent implements MongoTxTestInterface {
  @Autowired
  public MongoTxTestService(Mongo mongo, MongoDbFactory mongoDbFactory) {
    super(mongo, mongoDbFactory);
  }

  @MongoTx(writeConcern = "REPLICAS_SAFE")
  @Override
  public void txMethodNativeMongo(final WriteConcern expectedWriteConcern, final ReadPreference expectedReadPref) {
    super.txMethodNativeMongo(expectedWriteConcern, expectedReadPref);
  }

}
