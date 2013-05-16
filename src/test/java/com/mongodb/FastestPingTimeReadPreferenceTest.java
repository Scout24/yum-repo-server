package com.mongodb;

import org.junit.Before;
import org.junit.Test;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Random;
import static com.mongodb.ReplicaSetStatus.ReplicaSet;
import static com.mongodb.ReplicaSetStatus.ReplicaSetNode;
import static java.util.Arrays.asList;
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
    ReplicaSet replicaSet = new ReplicaSet(Collections.<ReplicaSetNode>emptyList(), new Random(), 12);

    assertThat(testedObject.getNode(replicaSet), is(nullValue()));
  }

  @Test
  public void getNodeWithLowestPingTime() throws UnknownHostException {
    ReplicaSetNode node1 = createReplicaSetNodeWithPingtime(10.0f);
    ReplicaSetNode node2 = createReplicaSetNodeWithPingtime(9.99f);
    ReplicaSetNode node3 = createReplicaSetNodeWithPingtime(11.0f);

    ReplicaSet replicaSet = new ReplicaSet(asList(node1, node2, node3), new Random(), 12);

    assertThat(testedObject.getNode(replicaSet), is(node2));

  }

  @Test
  public void getOneNodeWhenThereAreNodesWithSamePingTime() throws UnknownHostException {
    ReplicaSetNode node1 = createReplicaSetNodeWithPingtime(10.0f);
    ReplicaSetNode node2 = createReplicaSetNodeWithPingtime(10.0f);
    ReplicaSetNode node3 = createReplicaSetNodeWithPingtime(11.0f);

    ReplicaSet replicaSet = new ReplicaSet(asList(node1, node2, node3), new Random(), 12);

    assertThat(testedObject.getNode(replicaSet), isOneOf(node1, node2));
  }

  @Test
  public void getNotTheNodeWhichIsInStartupMode() throws UnknownHostException {
    ReplicaSetNode node1 = createReplicaSetNodeWithPingtime("127.0.0.1", 0.1f, STATE.STARTUP2);
    ReplicaSetNode node2 = createReplicaSetNodeWithPingtime("127.0.0.1", 0.5f, STATE.SECONDARY);
    ReplicaSetNode node3 = createReplicaSetNodeWithPingtime(11.0f);

    ReplicaSet replicaSet = new ReplicaSet(asList(node1, node2, node3), new Random(), 12);

    assertThat(testedObject.getNode(replicaSet), is(node2));
  }

  private ReplicaSetNode createReplicaSetNodeWithPingtime(float pingTime) throws UnknownHostException {
    return createReplicaSetNodeWithPingtime("127.0.0.1", pingTime);
  }

  private ReplicaSetNode createReplicaSetNodeWithPingtime(String hostname, float pingTime) throws UnknownHostException {
    return createReplicaSetNodeWithPingtime(hostname, pingTime, STATE.SECONDARY);
  }

  private ReplicaSetNode createReplicaSetNodeWithPingtime(String hostname, float pingTime, STATE state)
                                                   throws UnknownHostException {
    Random random = new Random();

    return new ReplicaSetNode(new ServerAddress(hostname, random.nextInt(5000) + 1024), new HashSet<String>(), "name",
      pingTime,
      true,
      state.isMaster(),
      state.isSecondary(),
      new LinkedHashMap<String, String>(), 0);
  }

  /**
   * @see http://docs.mongodb.org/manual/reference/command/replSetGetStatus/#dbcmd.replSetGetStatus
   */
  private static enum STATE {
    STARTUP,
    PRIMARY(true, false),
    SECONDARY(false, true),
    RECOVERING,
    FATAL,
    STARTUP2,
    UNKNOWN,
    ARBITER,
    DOWN,
    ROLLBACK,
    SHUNNED;

    private final boolean master;
    private final boolean secondary;

    STATE() {
      this(false, false);
    }

    STATE(final boolean master, final boolean secondary) {
      this.master = master;
      this.secondary = secondary;
    }

    private boolean isMaster() {
      return master;
    }

    private boolean isSecondary() {
      return secondary;
    }
  }
}
