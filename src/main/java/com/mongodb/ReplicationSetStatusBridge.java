package com.mongodb;

import com.google.common.base.Joiner;
import java.util.List;


/**
 * User: lgohlke
 */
public class ReplicationSetStatusBridge {
  private final ReplicaSetStatus replicaSetStatus;

  public ReplicationSetStatusBridge(final ReplicaSetStatus replicaSetStatus) {
    this.replicaSetStatus = replicaSetStatus;
  }

  public String getState(ServerAddress serverAddress) {
    List<ReplicaSetStatus.ReplicaSetNode> nodes = replicaSetStatus._replicaSetHolder.get().getAll();
    for (ReplicaSetStatus.ReplicaSetNode node : nodes) {
      if (node.getServerAddress().equals(serverAddress)) {
        String state = node.secondary() ? "secondary" : (node.master() ? "master" : node.toJSON());
        String pingTime = String.format("%.3f ms", node.getPingTime());
        String tags = Joiner.on(",").join(node.getTags());
        return Joiner.on("\t| ").join(state, pingTime, tags);
      }
    }
    return "---";
  }
}
