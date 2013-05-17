package de.is24.infrastructure.gridfs.http.mongo.util;

import de.flapdoodle.embed.mongo.Paths;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.distribution.BitSize;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.distribution.Platform;
import org.junit.Test;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import static de.flapdoodle.embed.mongo.Command.MongoD;
import static org.fest.assertions.api.Assertions.assertThat;


public class LocalMongoFactoryTest {
  private static final Version VERSION = Version.V2_4_1;

  @Test
  public void shouldHaveCorrectPath() {
    Distribution distribution = new Distribution(VERSION, Platform.Linux, BitSize.B64);
    String path = new Paths(MongoD).getPath(distribution);

    String downloadPath = "linux/mongodb-linux-x86_64-" + VERSION.asInDownloadPath() + ".tgz";
    assertThat(downloadPath).isEqualTo(path);
  }

  @Test
  public void shouldDownloadMongoIntoTempDir() throws IOException {
    // download happens here
    LocalMongoFactory.createMongoStarter().prepare(new MongodConfig(VERSION));

    String pathInTempDirectory = new Paths(MongoD).getPath(Distribution.detectFor(VERSION));
    String pathToDownload = LocalMongoFactory.MONGO_DOWNLOAD_FOLDER.asFile().getAbsolutePath() + File.separator +
      pathInTempDirectory;
    File downloadedMongoArchive = new File(pathToDownload);

    assertThat(downloadedMongoArchive).exists();
    assertThat(downloadedMongoArchive).isFile();
    assertThat(downloadedMongoArchive.getTotalSpace()).isGreaterThan(1024 * 1024L);
  }

  @Test(expected = SocketException.class)
  public void shouldStartAndStopMongoD() throws Throwable {
    MongoProcessHolder mongoProcess = LocalMongoFactory.createMongoProcess();
    mongoProcess.stopMongo();

    new Socket().connect(new InetSocketAddress(mongoProcess.getMongoPort()), 1);
  }
}
