package com.mongodb;

import com.mongodb.connection.ClusterDescription;
import com.mongodb.connection.ServerDescription;
import com.mongodb.connection.ServerType;
import org.junit.Before;
import org.junit.Test;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Random;

import static com.mongodb.connection.ClusterConnectionMode.MULTIPLE;
import static com.mongodb.connection.ClusterConnectionMode.SINGLE;
import static com.mongodb.connection.ClusterType.REPLICA_SET;
import static com.mongodb.connection.ClusterType.STANDALONE;
import static com.mongodb.connection.ServerConnectionState.CONNECTED;
import static com.mongodb.connection.ServerDescription.builder;
import static com.mongodb.connection.ServerType.REPLICA_SET_OTHER;
import static com.mongodb.connection.ServerType.REPLICA_SET_SECONDARY;
import static java.lang.Math.round;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.core.IsNull.nullValue;


public class FastestPingTimeReadPreferenceTest {
  private FastestPingTimeReadPreference testedObject;

  @Before
  public void setUp() throws Exception {
    testedObject = new FastestPingTimeReadPreference();

  }

  @Test
  public void getNullWhenNoReplicaSetsAvailable() {
    ClusterDescription clusterDescription = new ClusterDescription(SINGLE, STANDALONE, Collections.<ServerDescription>emptyList());
    assertThat(testedObject.choose(clusterDescription), is(nullValue()));
  }

  @Test
  public void getNodeWithLowestPingTime() throws UnknownHostException {
    ServerDescription node1 = createServerDescriptionWithPingtime(10.0f);
    ServerDescription node2 = createServerDescriptionWithPingtime(9.99f);
    ServerDescription node3 = createServerDescriptionWithPingtime(11.0f);

    ClusterDescription clusterDescription = new ClusterDescription(MULTIPLE, REPLICA_SET, asList(node1, node2, node3));

    assertThat(testedObject.choose(clusterDescription).get(0), is(node2));

  }

  @Test
  public void getOneNodeWhenThereAreNodesWithSamePingTime() throws UnknownHostException {
    ServerDescription node1 = createServerDescriptionWithPingtime(10.0f);
    ServerDescription node2 = createServerDescriptionWithPingtime(10.0f);
    ServerDescription node3 = createServerDescriptionWithPingtime(11.0f);

    ClusterDescription clusterDescription = new ClusterDescription(MULTIPLE, REPLICA_SET, asList(node1, node2, node3));

    assertThat(testedObject.choose(clusterDescription).get(0), isOneOf(node1, node2));
  }

  @Test
  public void getNotTheNodeWhichIsInStartupMode() throws UnknownHostException {
    ServerDescription node1 = createServerDescriptionWithPingtime("127.0.0.1", 0.1f, REPLICA_SET_OTHER);
    ServerDescription node2 = createServerDescriptionWithPingtime("127.0.0.1", 0.5f);
    ServerDescription node3 = createServerDescriptionWithPingtime(11.0f);

    ClusterDescription clusterDescription = new ClusterDescription(MULTIPLE, REPLICA_SET, asList(node1, node2, node3));

    assertThat(testedObject.choose(clusterDescription).get(0), is(node2));
  }

  private ServerDescription createServerDescriptionWithPingtime(float pingTime) throws UnknownHostException {
    return createServerDescriptionWithPingtime("127.0.0.1", pingTime);
  }

  private ServerDescription createServerDescriptionWithPingtime(String hostname, float pingTime) throws UnknownHostException {
    return createServerDescriptionWithPingtime(hostname, pingTime, REPLICA_SET_SECONDARY);
  }

  private ServerDescription createServerDescriptionWithPingtime(String hostname, float pingTime, ServerType type)
                                                   throws UnknownHostException {
    ServerDescription.Builder builder = builder()
        .address(new ServerAddress(hostname, new Random()
        .nextInt(5000) + 1024))
        .setName("name")
        .roundTripTime(round(pingTime * 1000), NANOSECONDS)
        .ok(true)
        .state(CONNECTED)
        .type(type);
    return builder.build();
  }
}
