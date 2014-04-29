package de.is24.infrastructure.gridfs.http.mongo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MongoStringToJsonConverterTest {

  @Test
  public void shouldParseParts() throws Exception {
    assertMatches("hosts=[server01:27017, server02:27017, server03:27017]", "hosts:['server01:27017', 'server02:27017', 'server03:27017']");
    assertMatches(
        "Type{t=Re, cM=Mu, all=[SD{address=server01:27017, t=RepSec, hosts=[server01:27017, server02:27017, server03:27017], p=[], a=[], pr='server02:27017',max=48000, tags={}, n='dev-server01'}}",
        "{t:'Re', cM:'Mu', all:[{address:'server01:27017', t:'RepSec', hosts:['server01:27017', 'server02:27017', 'server03:27017'], p:[], a:[], pr:'server02:27017',max:48000, tags:{}, n:'dev-server01'}}"
    );
  }

  @Test
  public void shouldParseReplicaSetInfo() throws Exception {
    assertMatches(
        "ReplicaSetStatus{name=dev-cluster01, cluster=ClusterDescription{type=ReplicaSet, connectionMode=Multiple, all=[ServerDescription{address=server01.local:27017, type=ReplicaSetSecondary, hosts=[server01.local:27017, server02.local:27017, server03.local:27017], passives=[], arbiters=[], primary='server02.local:27017', maxDocumentSize=16777216, maxMessageSize=48000000, maxWriteBatchSize=512, tags={}, setName='dev-cluster01', setVersion='null', averagePingTimeNanos=80556000, ok=true, state=Connected, version=ServerVersion{versionList=[2, 4, 3]}, minWireVersion=0, maxWireVersion=0}, ServerDescription{address=server02.local:27017, type=ReplicaSetPrimary, hosts=[server01.local:27017, server02.local:27017, server03.local:27017], passives=[], arbiters=[], primary='server02.local:27017', maxDocumentSize=16777216, maxMessageSize=48000000, maxWriteBatchSize=512, tags={}, setName='dev-cluster01', setVersion='null', averagePingTimeNanos=88612555, ok=true, state=Connected, version=ServerVersion{versionList=[2, 4, 3]}, minWireVersion=0, maxWireVersion=0}, ServerDescription{address=server03.local:27017, type=ReplicaSetSecondary, hosts=[server01.local:27017, server02.local:27017, server03.local:27017], passives=[], arbiters=[], primary='server02.local:27017', maxDocumentSize=16777216, maxMessageSize=48000000, maxWriteBatchSize=512, tags={}, setName='dev-cluster01', setVersion='null', averagePingTimeNanos=102106444, ok=true, state=Connected, version=ServerVersion{versionList=[2, 4, 3]}, minWireVersion=0, maxWireVersion=0}]}}",
        "{name:'dev-cluster01', cluster:{type:'ReplicaSet', connectionMode:'Multiple', all:[{address:'server01.local:27017', type:'ReplicaSetSecondary', hosts:['server01.local:27017', 'server02.local:27017', 'server03.local:27017'], passives:[], arbiters:[], primary:'server02.local:27017', maxDocumentSize:16777216, maxMessageSize:48000000, maxWriteBatchSize:512, tags:{}, setName:'dev-cluster01', setVersion:'null', averagePingTimeNanos:80556000, ok:'true', state:'Connected', version:{versionList:[2, 4, 3]}, minWireVersion:0, maxWireVersion:0}, {address:'server02.local:27017', type:'ReplicaSetPrimary', hosts:['server01.local:27017', 'server02.local:27017', 'server03.local:27017'], passives:[], arbiters:[], primary:'server02.local:27017', maxDocumentSize:16777216, maxMessageSize:48000000, maxWriteBatchSize:512, tags:{}, setName:'dev-cluster01', setVersion:'null', averagePingTimeNanos:88612555, ok:'true', state:'Connected', version:{versionList:[2, 4, 3]}, minWireVersion:0, maxWireVersion:0}, {address:'server03.local:27017', type:'ReplicaSetSecondary', hosts:['server01.local:27017', 'server02.local:27017', 'server03.local:27017'], passives:[], arbiters:[], primary:'server02.local:27017', maxDocumentSize:16777216, maxMessageSize:48000000, maxWriteBatchSize:512, tags:{}, setName:'dev-cluster01', setVersion:'null', averagePingTimeNanos:102106444, ok:'true', state:'Connected', version:{versionList:[2, 4, 3]}, minWireVersion:0, maxWireVersion:0}]}}"
    );
  }

  private void assertMatches(String input, String expectedOutput) {
    assertEquals(expectedOutput, new MongoStringToJsonConverter().convert(input));
  }
}