package de.is24.infrastructure.gridfs.http.mongo;

import com.mongodb.Mongo;
import com.mongodb.gridfs.GridFS;
import de.is24.infrastructure.gridfs.http.gridfs.GridFsService;
import de.is24.infrastructure.gridfs.http.metadata.MetadataService;
import de.is24.infrastructure.gridfs.http.metadata.RepoEntriesRepository;
import de.is24.infrastructure.gridfs.http.metadata.YumEntriesRepository;
import de.is24.infrastructure.gridfs.http.metadata.generation.RepoMdGenerator;
import de.is24.infrastructure.gridfs.http.repos.RepoCleaner;
import de.is24.infrastructure.gridfs.http.repos.RepoService;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;


public class IntegrationTestContext extends MongoTestContext {
  public static final String RPM_DB = "rpm_db";

  private MongoTemplate mongoTemplate;
  private GridFS gridFs;
  private GridFsTemplate gridFsTemplate;
  private GridFsService gridFsService;
  private YumEntriesRepository yumEntriesRepository;
  private RepoEntriesRepository repoEntriesRepository;
  private RepoService repoService;
  private RepoCleaner repoCleaner;

  private RepoMdGenerator repoMdGenerator;

  private MetadataService metadataService;

  public GridFS gridFs() {
    if (gridFs == null) {
      gridFs = new GridFS(getMongo().getDB(RPM_DB));
    }
    return gridFs;
  }

  public GridFsService gridFsService() {
    if (gridFsService == null) {
      gridFsService = new GridFsService(gridFs(), gridFsTemplate(), mongoTemplate(), yumEntriesRepository(),
        repoService());
    }
    return gridFsService;
  }

  public YumEntriesRepository yumEntriesRepository() {
    if (yumEntriesRepository == null) {
      yumEntriesRepository = new MongoRepositoryFactory(mongoTemplate()).getRepository(YumEntriesRepository.class);
    }
    return yumEntriesRepository;
  }

  public MongoTemplate mongoTemplate() {
    if (mongoTemplate == null) {
      mongoTemplate = mongoTemplate(getMongo());
    }
    return mongoTemplate;
  }

  public GridFsTemplate gridFsTemplate() {
    if (gridFsTemplate == null) {
      gridFsTemplate = gridFsTemplate(getMongo());
    }
    return gridFsTemplate;
  }

  public RepoEntriesRepository repoEntriesRepository() {
    if (repoEntriesRepository == null) {
      repoEntriesRepository = new MongoRepositoryFactory(mongoTemplate()).getRepository(RepoEntriesRepository.class);
    }
    return repoEntriesRepository;
  }

  public RepoService repoService() {
    if (repoService == null) {
      repoService = new RepoService(repoEntriesRepository());
    }

    return repoService;
  }

  public RepoCleaner repoCleaner() {
    if (repoCleaner == null) {
      repoCleaner = new RepoCleaner(mongoTemplate(), yumEntriesRepository(), gridFs(), repoService());
    }

    return repoCleaner;
  }

  public RepoMdGenerator repoMdGenerator() {
    if (repoMdGenerator == null) {
      repoMdGenerator = new RepoMdGenerator(gridFs());
    }
    return repoMdGenerator;
  }

  public MetadataService metadataService() {
    if (metadataService == null) {
      metadataService = new MetadataService(gridFsService(), yumEntriesRepository(), repoMdGenerator(),
        repoService(), repoCleaner());
    }
    return metadataService;
  }

  public static GridFsTemplate gridFsTemplate(Mongo mongo) {
    SimpleMongoDbFactory dbFactory = new SimpleMongoDbFactory(mongo, RPM_DB);
    return new GridFsTemplate(dbFactory, new MappingMongoConverter(dbFactory, new MongoMappingContext()));
  }

  public static MongoTemplate mongoTemplate(Mongo mongo) {
    SimpleMongoDbFactory dbFactory = new SimpleMongoDbFactory(mongo, RPM_DB);
    return new MongoTemplate(dbFactory);
  }
}
