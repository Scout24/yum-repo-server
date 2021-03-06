package de.is24.infrastructure.gridfs.http.repos;

import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import de.is24.infrastructure.gridfs.http.domain.RepoEntry;
import de.is24.infrastructure.gridfs.http.metadata.YumEntriesRepository;
import de.is24.infrastructure.gridfs.http.rpm.version.CachingVersionDBObjectComparator;
import de.is24.infrastructure.gridfs.http.storage.FileStorageService;
import de.is24.util.monitoring.spring.TimeMeasurement;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static de.is24.infrastructure.gridfs.http.domain.RepoType.SCHEDULED;
import static de.is24.infrastructure.gridfs.http.domain.RepoType.STATIC;
import static de.is24.infrastructure.gridfs.http.mongo.MongoAggregationBuilder.field;
import static de.is24.infrastructure.gridfs.http.mongo.MongoAggregationBuilder.groupBy;
import static de.is24.infrastructure.gridfs.http.mongo.MongoAggregationBuilder.match;
import static java.util.Arrays.asList;
import static java.util.Collections.sort;
import static org.springframework.data.mongodb.core.query.Criteria.where;


@ManagedResource
@Service
@TimeMeasurement
public class RepoCleaner {
  public static final String REPO_KEY = "repo";
  private static final Logger LOG = LoggerFactory.getLogger(RepoCleaner.class);
  public static final String VERSION_KEY = "version";
  public static final String TIME_KEY = "time";
  public static final String TIME_BUILD_KEY = "build";
  public static final String FILE_KEY = "file";
  public static final String FILENAME_KEY = "filename";
  public static final String ITEMS_KEY = "items";
  private final MongoTemplate mongo;
  private final YumEntriesRepository entriesRepository;
  private final FileStorageService fileStorageService;
  private final RepoService repoService;
  private final CachingVersionDBObjectComparator comparatorVersion = new CachingVersionDBObjectComparator();

  /* for CGLIB */
  protected RepoCleaner() {
    mongo = null;
    entriesRepository = null;
    fileStorageService = null;
    repoService = null;
  }

  @Autowired
  public RepoCleaner(MongoTemplate mongo, YumEntriesRepository entriesRepository, FileStorageService fileStorageService,
                     RepoService repoService) {
    this.mongo = mongo;
    this.entriesRepository = entriesRepository;
    this.fileStorageService = fileStorageService;
    this.repoService = repoService;
  }

    @ManagedOperation
    public boolean cleanByMaxNum(String reponame, int maxValue) {
        if (maxValue > 0) {
            LOG.info("Cleaning up repository {} and keep {} rpms at maximum ...", reponame, maxValue);

            AggregationOutput aggregation = aggregateAllRpmNamesInRepoThatHaveMoreThanMaxKeepEntries(reponame, maxValue);
            boolean filesDeleted = false;

            for (DBObject aggregatedArtifact : aggregation.results()) {
                for (DBObject itemToDelete : oldestItemsToDeleteByVersion(maxValue, getItemsFromAggregate(aggregatedArtifact))) {
                    ObjectId fileId = (ObjectId) itemToDelete.get(FILE_KEY);
                    if (fileId != null) {
                        entriesRepository.delete(fileId);

                        final String path = reponame + "/" + itemToDelete.get(FILENAME_KEY);
                        fileStorageService.markForDeletionByPath(path);
                        filesDeleted = true;
                        LOG.info("Mark file {} as deleted during cleanup.", path);
                    }
                }
            }

            LOG.info("Clean up for repository {} finished.", reponame);

            if (filesDeleted) {
                repoService.createOrUpdate(reponame);
                return true;
            }
        }
        return false;
    }

    @ManagedOperation
    public boolean cleanByMaxDays(String reponame, int maxValue) {
        if (maxValue > 0) {
            LOG.info("Cleaning up repository {} and keep rpms newer than {} days ...", reponame, maxValue);

            AggregationOutput aggregation = aggregateAllRpmNamesInRepoByTime(reponame);
            boolean filesDeleted = false;

            for (DBObject aggregatedArtifact : aggregation.results()) {
                for (DBObject itemToDelete : oldestItemsToDeleteByDays(maxValue, getItemsFromAggregate(aggregatedArtifact))) {
                    ObjectId fileId = (ObjectId) itemToDelete.get(FILE_KEY);
                    if (fileId != null) {
                        entriesRepository.delete(fileId);

                        final String path = reponame + "/" + itemToDelete.get(FILENAME_KEY);
                        fileStorageService.markForDeletionByPath(path);
                        filesDeleted = true;
                        LOG.info("Mark file {} as deleted during cleanup.", path);
                    }
                }
            }

            LOG.info("Clean up for repository {} finished.", reponame);

            if (filesDeleted) {
                repoService.createOrUpdate(reponame);
                return true;
            }
        }
        return false;
    }

  @SuppressWarnings("unchecked")
  private List<DBObject> getItemsFromAggregate(final DBObject aggregatedArtifact) {
    return (List<DBObject>) aggregatedArtifact.get(ITEMS_KEY);
  }

  @ManagedOperation
  public boolean cleanup(String reponame) {
   RepoEntry repoEntry = repoService.ensureEntry(reponame, STATIC, SCHEDULED);
   return (cleanByMaxNum(reponame, repoEntry.getMaxKeepRpms()) | cleanByMaxDays(reponame, repoEntry.getMaxDaysRpms()));
  }

  private AggregationOutput aggregateAllRpmNamesInRepoByTime(String reponame) {
    BasicDBObject repoMatch = match(where(REPO_KEY).is(reponame));
    BasicDBObject groupArtifactNames = groupBy(field("name", "yumPackage.name"), field("arch", "yumPackage.arch")).push(
      ITEMS_KEY,
      field(TIME_KEY, "yumPackage.time"),
      field(FILE_KEY, "_id"),
      field(FILENAME_KEY, "yumPackage.location.href"))
      .build();

      return mongo.getCollection("yum.entries").aggregate(asList(repoMatch, groupArtifactNames));
  }

  private AggregationOutput aggregateAllRpmNamesInRepoThatHaveMoreThanMaxKeepEntries(String reponame, int maxKeepRpm) {
    List<DBObject> pipeline = new ArrayList<>();
    pipeline.add(match(where(REPO_KEY).is(reponame)));
    pipeline.add(groupBy(field("name", "yumPackage.name"), field("arch", "yumPackage.arch")).push(
      ITEMS_KEY,
      field(VERSION_KEY, "yumPackage.version"),
      field(FILE_KEY, "_id"),
      field(FILENAME_KEY, "yumPackage.location.href"))
      .count()
      .build());
    pipeline.add(match(where("count").gt(maxKeepRpm)));

    return mongo.getCollection("yum.entries").aggregate(pipeline);
  }

  private List<DBObject> oldestItemsToDeleteByVersion(int maxKeepRpm, List<DBObject> items) {
    sort(items, (DBObject obj1, DBObject obj2) -> comparatorVersion.compare(obj1.get(VERSION_KEY), obj2.get(VERSION_KEY)));
    return items.subList(0, items.size() - maxKeepRpm);
  }

  private List<DBObject> oldestItemsToDeleteByDays(int maxDaysRpm, List<DBObject> items) {
    DateTime startDate = new DateTime().minusDays(maxDaysRpm).withZoneRetainFields(DateTimeZone.UTC);
    List<DBObject> newList = new LinkedList<>();
    for (DBObject item : items) {
      BasicDBObject aux = (BasicDBObject) item.get(TIME_KEY);
      Long itemTime = aux.getLong(TIME_BUILD_KEY);
      DateTime itemDateTime = new DateTime(itemTime * 1000L).withZoneRetainFields(DateTimeZone.UTC);
      if (itemDateTime.isBefore(startDate)) {
        newList.add(item);
      }
    }
    return newList;
  }
}
