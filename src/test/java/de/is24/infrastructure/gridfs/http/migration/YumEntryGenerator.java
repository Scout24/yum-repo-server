package de.is24.infrastructure.gridfs.http.migration;

import com.mongodb.Mongo;
import com.mongodb.gridfs.GridFSDBFile;
import de.is24.infrastructure.gridfs.http.domain.YumEntry;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackage;
import de.is24.infrastructure.gridfs.http.exception.InvalidRpmHeaderException;
import de.is24.infrastructure.gridfs.http.rpm.RpmHeaderToYumPackageConverter;
import de.is24.infrastructure.gridfs.http.rpm.RpmHeaderWrapper;
import de.is24.infrastructure.gridfs.http.metadata.YumEntriesRepository;
import org.bson.types.ObjectId;
import org.freecompany.redline.ReadableChannelWrapper;
import org.freecompany.redline.Scanner;
import org.freecompany.redline.header.Header;
import org.springframework.data.authentication.UserCredentials;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.List;

import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.REPO_KEY;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.YUM_ENTRY_COLLECTION;
import static java.nio.channels.Channels.newChannel;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.gridfs.GridFsCriteria.whereMetaData;

/**
 * Class to regenerate all YumEntries for all RPMs
 */
public class YumEntryGenerator {

  public static final String DB_NAME = "rpm_db";
  private static final String SERVER_ADDRESS = "bergfs01.be.ber.is24.loc";
  public static final String USERNAME = "reposerver";
  public static final String PASSWORD = "2938!21*373$_2gE,uZ";
  private final GridFsTemplate gridFs;
  private final Mongo mongo;
  private final SimpleMongoDbFactory dbFactory;
  private final MongoTemplate mongoTemplate;
  private final YumEntriesRepository yumEntriesRepository;


  public YumEntryGenerator() throws UnknownHostException {
    mongo = new Mongo(SERVER_ADDRESS);
    dbFactory = new SimpleMongoDbFactory(mongo, DB_NAME, new UserCredentials(USERNAME, PASSWORD));
    gridFs = new GridFsTemplate(dbFactory, new MappingMongoConverter(dbFactory, new MongoMappingContext()));
    mongoTemplate = new MongoTemplate(dbFactory);
    yumEntriesRepository = new MongoRepositoryFactory(mongoTemplate).getRepository(YumEntriesRepository.class);
  }

  public static void main(String[] args) throws Exception {
    new YumEntryGenerator().generate();
  }

  private void generate() throws InvalidRpmHeaderException, IOException {
    mongoTemplate.getCollection(YUM_ENTRY_COLLECTION).ensureIndex(REPO_KEY);
    FileOutputStream fos = new FileOutputStream("migrate.log");
    try {
      List<GridFSDBFile> gridFSDBFiles = gridFs.find(query(whereMetaData(REPO_KEY).exists(true)));
      int count = 1;
      int size = gridFSDBFiles.size();
      for (GridFSDBFile dbFile : gridFSDBFiles) {
        if (yumEntriesRepository.findOne((ObjectId) dbFile.getId()) == null) {
          yumEntriesRepository.save(new YumEntry((ObjectId) dbFile.getId(), getRepo(dbFile), getYumPackage(dbFile)));
          write(fos, count + " / " + size + " Saved " + dbFile.getFilename());
        } else {
          write(fos, count + " / " + size + " Skip " + dbFile.getFilename());
        }
        count++;
      }
    } finally {
      fos.close();
    }
  }

  private void write(OutputStream outputStream, String msg) throws IOException {
    outputStream.write(msg.concat("\n").getBytes());
  }

  private String getRepo(GridFSDBFile dbFile) {
    return dbFile.getMetaData().get(REPO_KEY).toString();
  }

  private YumPackage getYumPackage(GridFSDBFile dbFile) throws InvalidRpmHeaderException, IOException {
    InputStream inputStream = dbFile.getInputStream();
    RpmHeaderWrapper headerWrapper = new RpmHeaderWrapper(readHeader(inputStream));
    inputStream.close();
    RpmHeaderToYumPackageConverter converter = new RpmHeaderToYumPackageConverter(headerWrapper);
    return converter.convert();
  }

  private Header readHeader(InputStream inputStream) throws InvalidRpmHeaderException {
    ReadableChannelWrapper channel = new ReadableChannelWrapper(newChannel(inputStream));
    try {
      return new Scanner().run(channel).getHeader();
    } catch (Exception e) {
      throw new InvalidRpmHeaderException("Could not read rpm header.", e);
    }
  }
}
