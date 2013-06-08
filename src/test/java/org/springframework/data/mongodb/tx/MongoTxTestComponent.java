package org.springframework.data.mongodb.tx;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.stereotype.Service;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


@Service
public class MongoTxTestComponent {
  private final DB dbFromNativeMongo;
  private final DB dbFromFactory;

  public MongoTxTestComponent() {
    dbFromFactory = null;
    dbFromNativeMongo = null;
  }

  @Autowired
  public MongoTxTestComponent(final Mongo mongo, final MongoDbFactory mongoDbFactory) {
    dbFromNativeMongo = mongo.getDB("rpm_db");
    dbFromFactory = mongoDbFactory.getDb();
  }

  @MongoTx(readPreference = "nearest", writeConcern = "REPLICAS_SAFE")
  public void txMethodNativeMongo(final WriteConcern expectedWriteConcern, final ReadPreference expectedReadPref) {
    expectMongoConfigApplied(expectedWriteConcern, expectedReadPref, dbFromNativeMongo);
  }

  @MongoTx
  public void txMethodNativeMongoWithDefaults(final WriteConcern expectedWriteConcern,
                                              final ReadPreference expectedReadPref) {
    expectMongoConfigApplied(expectedWriteConcern, expectedReadPref, dbFromNativeMongo);
  }

  @MongoTx(readPreference = "nearest", writeConcern = "REPLICAS_SAFE")
  public void txMethodMongoFactory(final WriteConcern expectedWriteConcern, final ReadPreference expectedReadPref) {
    expectMongoConfigApplied(expectedWriteConcern, expectedReadPref, dbFromFactory);
  }

  public void nonTxMethodNativeMongo(final WriteConcern expectedWriteConcern, final ReadPreference expectedReadPref) {
    expectMongoConfigApplied(expectedWriteConcern, expectedReadPref, dbFromNativeMongo);
  }

  public void nonTxMethodMongoFactory(final WriteConcern expectedWriteConcern, final ReadPreference expectedReadPref) {
    expectMongoConfigApplied(expectedWriteConcern, expectedReadPref, dbFromFactory);
  }

  private void expectMongoConfigApplied(WriteConcern expectedWriteConcern, ReadPreference expectedReadPref, DB db1) {
    assertThat(db1.getReadPreference().getName(), is(expectedReadPref.getName()));
    assertThat(db1.getWriteConcern(), is(equalTo(expectedWriteConcern)));
  }
}
