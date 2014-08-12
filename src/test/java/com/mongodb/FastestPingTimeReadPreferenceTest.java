package com.mongodb;

import org.junit.Before;
import org.junit.Test;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Random;

import static com.mongodb.ClusterConnectionMode.Multiple;
import static com.mongodb.ClusterConnectionMode.Single;
import static com.mongodb.ClusterType.ReplicaSet;
import static com.mongodb.ClusterType.StandAlone;
import static com.mongodb.ServerConnectionState.Connected;
import static com.mongodb.ServerDescription.builder;
import static com.mongodb.ServerType.ReplicaSetOther;
import static com.mongodb.ServerType.ReplicaSetSecondary;
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
    ClusterDescription clusterDescription = new ClusterDescription(Single, StandAlone, Collections.<ServerDescription>emptyList());
    assertThat(testedObject.choose(clusterDescription), is(nullValue()));
  }

  @Test
  public void getNodeWithLowestPingTime() throws UnknownHostException {
    ServerDescription node1 = createServerDescriptionWithPingtime(10.0f);
    ServerDescription node2 = createServerDescriptionWithPingtime(9.99f);
    ServerDescription node3 = createServerDescriptionWithPingtime(11.0f);

    ClusterDescription clusterDescription = new ClusterDescription(Multiple, ReplicaSet, asList(node1, node2, node3));

    assertThat(testedObject.choose(clusterDescription).get(0), is(node2));

  }

  @Test
  public void getOneNodeWhenThereAreNodesWithSamePingTime() throws UnknownHostException {
    ServerDescription node1 = createServerDescriptionWithPingtime(10.0f);
    ServerDescription node2 = createServerDescriptionWithPingtime(10.0f);
    ServerDescription node3 = createServerDescriptionWithPingtime(11.0f);

    ClusterDescription clusterDescription = new ClusterDescription(Multiple, ReplicaSet, asList(node1, node2, node3));

    assertThat(testedObject.choose(clusterDescription).get(0), isOneOf(node1, node2));
  }

  @Test
  public void getNotTheNodeWhichIsInStartupMode() throws UnknownHostException {
    ServerDescription node1 = createServerDescriptionWithPingtime("127.0.0.1", 0.1f, ReplicaSetOther);
    ServerDescription node2 = createServerDescriptionWithPingtime("127.0.0.1", 0.5f);
    ServerDescription node3 = createServerDescriptionWithPingtime(11.0f);

    ClusterDescription clusterDescription = new ClusterDescription(Multiple, ReplicaSet, asList(node1, node2, node3));

    assertThat(testedObject.choose(clusterDescription).get(0), is(node2));
  }

  private ServerDescription createServerDescriptionWithPingtime(float pingTime) throws UnknownHostException {
    return createServerDescriptionWithPingtime("127.0.0.1", pingTime);
  }

  private ServerDescription createServerDescriptionWithPingtime(String hostname, float pingTime) throws UnknownHostException {
    return createServerDescriptionWithPingtime(hostname, pingTime, ReplicaSetSecondary);
  }

  private ServerDescription createServerDescriptionWithPingtime(String hostname, float pingTime, ServerType type)
                                                   throws UnknownHostException {
    ServerDescription.Builder builder = builder()
        .address(new ServerAddress(hostname, new Random()
        .nextInt(5000) + 1024))
        .setName("name")
        .averageLatency(round(pingTime * 1000), NANOSECONDS)
        .ok(true)
        .state(Connected)
        .type(type);
    return builder.build();
  }
}
