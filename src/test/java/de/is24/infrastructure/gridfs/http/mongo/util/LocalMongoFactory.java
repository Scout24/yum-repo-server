package de.is24.infrastructure.gridfs.http.mongo.util;

import com.google.common.annotations.VisibleForTesting;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.Paths;
import de.flapdoodle.embed.mongo.config.ArtifactStoreBuilder;
import de.flapdoodle.embed.mongo.config.DownloadConfigBuilder;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.io.directories.FixedPath;
import de.flapdoodle.embed.process.io.directories.PlatformTempDir;
import de.is24.infrastructure.gridfs.http.utils.retry.RetryUtils;
import java.io.File;
import java.net.UnknownHostException;
import static de.flapdoodle.embed.mongo.Command.MongoD;
import static de.is24.infrastructure.gridfs.http.utils.retry.RetryUtils.execute;
import static de.is24.infrastructure.gridfs.http.web.AbstractContainerAndMongoDBStarter.MONGO_PASSWORD;
import static de.is24.infrastructure.gridfs.http.web.AbstractContainerAndMongoDBStarter.MONGO_USERNAME;


public class LocalMongoFactory {
  private static final String TEMP_DIR = new PlatformTempDir().asFile().getAbsolutePath();
  @VisibleForTesting
  static final FixedPath MONGO_DOWNLOAD_FOLDER = new FixedPath(TEMP_DIR + File.separator + ".embedded-mongo");

  @VisibleForTesting
  static MongodStarter createMongoStarter() {
    Paths path = new Paths(MongoD);

    de.flapdoodle.embed.process.config.store.DownloadConfigBuilder downloadConfigBuilder =
      new DownloadConfigBuilder() //
      .defaultsForCommand(MongoD) //
      .downloadPath("http://fastdl.mongodb.org/") //
      .artifactStorePath(MONGO_DOWNLOAD_FOLDER).packageResolver(path);
    de.flapdoodle.embed.process.store.ArtifactStoreBuilder download =
      new ArtifactStoreBuilder() //
      .defaults(MongoD) //
      .download(downloadConfigBuilder);
    IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder().defaults(MongoD).artifactStore(download).build();
    return MongodStarter.getInstance(runtimeConfig);
  }

  public static MongoProcessHolder createMongoProcess() throws Throwable {
    final MongodStarter runtime = createMongoStarter();

    return execute().maxTries(3).wait(3).command(new RetryUtils.Retryable<MongoProcessHolder>() {
        @Override
        public MongoProcessHolder run() throws Throwable {
          MongodConfig config = new MongodConfig(getVersion());
          MongodExecutable mongodExecutable = runtime.prepare(config);
          MongodProcess mongoProcess = mongodExecutable.start();
          prepareDatabase(config.net().getPort());

          return new MongoProcessHolder(mongodExecutable, mongoProcess, config.net().getPort());
        }

        private Version getVersion() {
          String version = System.getProperty("embedded.mongodb.version", Version.Main.PRODUCTION.asInDownloadPath());
          return Version.valueOf("V" + version.replaceAll("\\.", "_"));
        }

        private void prepareDatabase(final int mongoPort) throws UnknownHostException {
          Mongo mongo = new MongoClient("localhost", mongoPort);
          DB db = mongo.getDB("rpm_db");
          db.addUser(MONGO_USERNAME, MONGO_PASSWORD.toCharArray());
        }
      });

  }
}
