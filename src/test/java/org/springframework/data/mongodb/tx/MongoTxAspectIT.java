package org.springframework.data.mongodb.tx;

import com.mongodb.FastestPingTimeReadPreference;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@ContextConfiguration(classes = { MongoTxTextContext.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class MongoTxAspectIT {
  @Autowired
  private MongoTxTestInterface mongoTxTestInterface;
  @Autowired
  private MongoTxTestComponent txTestComponent;

  @Test
  public void doInMongoTxForNativeMongo() throws Exception {
    txTestComponent.txMethodNativeMongo(WriteConcern.REPLICAS_SAFE, ReadPreference.nearest());
  }

  @Test
  public void outermostConfigRules() throws Exception {
    mongoTxTestInterface.txMethodNativeMongo(WriteConcern.REPLICAS_SAFE, ReadPreference.primary());
  }

  @Test
  public void doInMongoTxForNativeMongoWithDefaults() throws Exception {
    mongoTxTestInterface.txMethodNativeMongoWithDefaults(WriteConcern.JOURNALED, ReadPreference.primary());
  }

  @Test(expected = IllegalArgumentException.class)
  public void doInMongoTxForNativeMongoWithUnknownWriteConcern() throws Exception {
    mongoTxTestInterface.txMethodNativeMongoWithUnknownWriteConcern(WriteConcern.JOURNALED, ReadPreference.primary());
  }

  @Test
  public void doInMongoTxForMongoFactory() throws Exception {
    mongoTxTestInterface.txMethodMongoFactory(WriteConcern.REPLICAS_SAFE, ReadPreference.nearest());
  }

  @Test
  public void doWithoutMongoTxForNativeMongo() throws Exception {
    mongoTxTestInterface.nonTxMethodNativeMongo(WriteConcern.JOURNALED, new FastestPingTimeReadPreference());
  }

  @Test
  public void doWithoutMongoTxForMongoFactory() throws Exception {
    mongoTxTestInterface.nonTxMethodMongoFactory(WriteConcern.JOURNALED, new FastestPingTimeReadPreference());
  }
}
