package de.is24.infrastructure.gridfs.http.mongo;

import com.mongodb.Mongo;
import com.mongodb.ReplicaSetStatus;
import com.mongodb.ServerAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import static java.util.Arrays.asList;


@ManagedResource
@Service
public class MongoPrimaryDetector {
  private Mongo mongo;
  private String localHostname;
  private final ArrayList<InetAddress> hostAddresses = new ArrayList<>();

  @Autowired
  public MongoPrimaryDetector(Mongo mongo) {
    this.mongo = mongo;
    try {
      this.localHostname = InetAddress.getLocalHost().getHostName();
      hostAddresses.add(InetAddress.getLoopbackAddress());
      hostAddresses.addAll(asList(InetAddress.getAllByName(localHostname)));
    } catch (UnknownHostException e) {
      throw new IllegalStateException("Could not determine local hostname.", e);
    }
  }

  @ManagedOperation
  public boolean isPrimary() {
    ReplicaSetStatus replicaSetStatus = mongo.getReplicaSetStatus();

    if (replicaSetStatus == null) {
      return true;
    }

    InetAddress masterAddress = getMasterAddressFrom(replicaSetStatus);
    return hostAddresses.contains(masterAddress);
  }

  private InetAddress getMasterAddressFrom(ReplicaSetStatus replicaSetStatus) {
    final ServerAddress masterServer = replicaSetStatus.getMaster();
    if (masterServer == null) {
      return null;
    }
    return masterServer.getSocketAddress().getAddress();
  }

  @ManagedOperation
  public String getLocalHostname() {
    return localHostname;
  }
}
