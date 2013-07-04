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
  private MongoTxTestComponent testComponent;


  @Test
  public void doInMongoTxForNativeMongo() throws Exception {
    testComponent.txMethodNativeMongo(WriteConcern.REPLICAS_SAFE, ReadPreference.nearest());
  }

  @Test
  public void doInMongoTxForNativeMongoWithDefaults() throws Exception {
    testComponent.txMethodNativeMongoWithDefaults(WriteConcern.JOURNALED, ReadPreference.primary());
  }

  @Test(expected = IllegalArgumentException.class)
  public void doInMongoTxForNativeMongoWithUnknownWriteConcern() throws Exception {
    testComponent.txMethodNativeMongoWithUnknownWriteConcern(WriteConcern.JOURNALED, ReadPreference.primary());
  }

  @Test
  public void doInMongoTxForMongoFactory() throws Exception {
    testComponent.txMethodMongoFactory(WriteConcern.REPLICAS_SAFE, ReadPreference.nearest());
  }

  @Test
  public void doWithoutMongoTxForNativeMongo() throws Exception {
    testComponent.nonTxMethodNativeMongo(WriteConcern.JOURNALED, new FastestPingTimeReadPreference());
  }

  @Test
  public void doWithoutMongoTxForMongoFactory() throws Exception {
    testComponent.nonTxMethodMongoFactory(WriteConcern.JOURNALED, new FastestPingTimeReadPreference());
  }
}
