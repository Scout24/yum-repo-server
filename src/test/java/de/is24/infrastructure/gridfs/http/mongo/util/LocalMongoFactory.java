package de.is24.infrastructure.gridfs.http.mongo.util;

import static de.flapdoodle.embed.mongo.Command.MongoD;
import static de.is24.infrastructure.gridfs.http.utils.retry.RetryUtils.execute;

import com.mongodb.DB;
import com.mongodb.Mongo;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.ArtifactStoreBuilder;
import de.flapdoodle.embed.mongo.config.DownloadConfigBuilder;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.runtime.Network;
import de.is24.infrastructure.gridfs.http.utils.retry.RetryUtils;


public class LocalMongoFactory {
  public static MongoProcessHolder createMongoProcess() throws Throwable {
    MavenPaths mavenPaths = new MavenPaths(MongoD, "com.mongodb", "mongodb-binary");
    IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder().defaults(MongoD)
      .artifactStore(new ArtifactStoreBuilder().defaults(MongoD)
        .download(new DownloadConfigBuilder().defaultsForCommand(MongoD)
          .downloadPath("http://devnex.rz.is24.loc/content/groups/public/")
          .packageResolver(mavenPaths)))
      .build();
    final MongodStarter runtime = MongodStarter.getInstance(runtimeConfig);

    return execute().maxTries(3).wait(3).command(new RetryUtils.Retryable<MongoProcessHolder>() {
        @Override
        public MongoProcessHolder run() throws Throwable {
          final int mongoPort = Network.getFreeServerPort();
          final MongodExecutable mongoExe = runtime.prepare(
            new MongodConfig(Version.V2_3_0, mongoPort,
              Network.localhostIsIPv6()));
          MongodProcess mongoProcess = mongoExe.start();
          Mongo mongo = new Mongo("localhost", mongoPort);
          DB db = mongo.getDB("rpm_db");
          db.addUser("reposerver", "reposerver".toCharArray());

          return new MongoProcessHolder(mongoExe, mongoProcess, mongoPort);
        }
      });

  }
}
