package com.mongodb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import static com.mongodb.ReplicaSetStatus.ReplicaSetNode;


public class FastestPingTimeReadPreference extends ReadPreference {
  private static final Logger LOGGER = LoggerFactory.getLogger(FastestPingTimeReadPreference.class);

  @Override
  public boolean isSlaveOk() {
    return true;
  }

  @Override
  public DBObject toDBObject() {
    return new BasicDBObject("mode", getName());
  }

  @Override
  public String getName() {
    return "nearestNode";
  }

  @Override
  ReplicaSetNode getNode(ReplicaSetStatus.ReplicaSet set) {
    final List<ReplicaSetNode> nodeList = set.getAll();

    if (nodeList.isEmpty()) {
      return null;
    }

    final ReplicaSetNode replicaSetNode = selectNearestQueryableNode(nodeList);

    if (LOGGER.isDebugEnabled()) {
      StringBuilder buffer = new StringBuilder();
      for (ReplicaSetNode node : set.getAll()) {
        if (!node.equals(replicaSetNode)) {
          buffer.append("[");
          buffer.append(node.getServerAddress().getHost());
          buffer.append("/");
          buffer.append(node.getPingTime());
          buffer.append("] ");
        }
      }

      String choosenNode = replicaSetNode.getServerAddress().getHost() + "/" + replicaSetNode.getPingTime();
      LOGGER.debug("take {} as mongodb host. other {}", choosenNode, buffer.toString());
    } else {
      LOGGER.info("take {} as mongodb host",
        (replicaSetNode == null) ? "--" : replicaSetNode.getServerAddress().getHost());
    }
    return replicaSetNode;
  }


  private static ReplicaSetNode selectNearestQueryableNode(final Iterable<ReplicaSetNode> nodeList) {
    ReplicaSetNode nearest = null;
    for (ReplicaSetNode node : nodeList) {
      if (isQueryable(node)) {
        nearest = calculateNearest(nearest, node);
      }
    }
    return nearest;
  }

  private static ReplicaSetNode calculateNearest(final ReplicaSetNode nodeA, final ReplicaSetNode nodeB) {
    if (nodeA == null) {
      return nodeB;
    }

    if (Float.compare(nodeA.getPingTime(), nodeB.getPingTime()) > 0) {
      return nodeB;
    }
    return nodeA;
  }

  private static boolean isQueryable(final ReplicaSetNode node) {
    return node.isOk() && (node.secondary() || node.master());
  }
}
