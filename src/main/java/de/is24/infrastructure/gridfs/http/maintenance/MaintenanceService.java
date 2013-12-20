package de.is24.infrastructure.gridfs.http.maintenance;

import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFSDBFile;
import de.is24.infrastructure.gridfs.http.domain.YumEntry;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackage;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageReducedView;
import de.is24.infrastructure.gridfs.http.gridfs.GridFsFileDescriptor;
import de.is24.infrastructure.gridfs.http.gridfs.GridFsService;
import de.is24.infrastructure.gridfs.http.metadata.YumEntriesRepository;
import de.is24.infrastructure.gridfs.http.monitoring.TimeMeasurement;
import de.is24.infrastructure.gridfs.http.rpm.version.YumPackageVersionComparator;
import org.apache.log4j.MDC;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.DocumentCallbackHandler;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.YUM_ENTRY_COLLECTION;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.GRIDFS_FILES_COLLECTION;


@Service
@TimeMeasurement
public class MaintenanceService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceService.class);
  private final Filter obsoleteRpmFiler = new ObsoleteRpmFilter();
  private final Filter propagatableRpmFilter = new PropagatableRpmFilter();

  private YumPackageVersionComparator versionComparator = new YumPackageVersionComparator();
  private TaskScheduler taskScheduler;


  private YumEntriesRepository yumEntriesRepository;
  private GridFsService gridFsService;
  private MongoTemplate mongoTemplate;
  private GridFsOperations gridFsTemplate;

  /* for AOP autoproxying */
  protected MaintenanceService() {
  }


  @Autowired
  public MaintenanceService(TaskScheduler taskScheduler, YumEntriesRepository yumEntriesRepository,
                            GridFsService gridFsService, MongoTemplate mongoTemplate, GridFsOperations gridFsTemplate) {
    this.taskScheduler = taskScheduler;
    this.yumEntriesRepository = yumEntriesRepository;
    this.gridFsService = gridFsService;
    this.mongoTemplate = mongoTemplate;
    this.gridFsTemplate = gridFsTemplate;
  }

  public Set<YumPackageReducedView> getPropagatableRPMs(String targetRepo,
                                                        String sourceRepo) {
    return filterRPMsFromPropagationChain(propagatableRpmFilter, targetRepo, sourceRepo);
  }

  public Set<YumPackageReducedView> getObsoleteRPMs(String targetRepo,
                                                    String sourceRepo) {
    return filterRPMsFromPropagationChain(obsoleteRpmFiler, targetRepo, sourceRepo);
  }

  public void triggerDeletionOfObsoleteRPMs(String initiatorDescription, String targetRepo, String sourceRepo) {
    DeleteObsoleteRPMsJob job = new DeleteObsoleteRPMsJob(initiatorDescription, sourceRepo,
      targetRepo);
    taskScheduler.schedule(job, new Date());
    LOGGER.info("triggered delete obsolete RPMs in propagation chain from {} to {}", sourceRepo, targetRepo);
  }

  public Map<ObjectId, YumPackageReducedView> getYumEntriesWithoutAssociatedFiles() {
    final Query query = new Query();
    query.fields().include("_id");


    CheckForMissingFsFilesCallbackHandler callbackHandler = new CheckForMissingFsFilesCallbackHandler();
    mongoTemplate.executeQuery(query, YUM_ENTRY_COLLECTION, callbackHandler);

    Map<ObjectId, YumPackageReducedView> result = new HashMap<ObjectId, YumPackageReducedView>();
    for (ObjectId id : callbackHandler.getEntriesWithMissingFile()) {
      YumEntry yumEntry = yumEntriesRepository.findOne(id);
      result.put(yumEntry.getId(), new YumPackageReducedView(yumEntry.getYumPackage()));
    }
    return result;
  }

  public Set<GridFSDBFile> getFilesWithoutYumEntry() {
    final Query query = new Query(where("metadata.arch").ne("repodata"));
    query.addCriteria(where("metadata.markedAsDeleted").exists(false));

    CheckForMissingEntriesCallbackHandler callbackHandler = new CheckForMissingEntriesCallbackHandler();
    mongoTemplate.executeQuery(query, GRIDFS_FILES_COLLECTION, callbackHandler);

    Set<GridFSDBFile> result = new HashSet<GridFSDBFile>();
    for (ObjectId id : callbackHandler.getFilesWithMissingEntry()) {
      GridFSDBFile gridFSDBFile = gridFsTemplate.findOne(Query.query(where("_id").is(id)));
      result.add(gridFSDBFile);
    }
    return result;
  }


  private void deleteObsoleteRPMs(String targetRepo, String sourceRepo) {
    Set<YumPackageReducedView> obsoleteRPMs = filterRPMsFromPropagationChain(obsoleteRpmFiler, targetRepo, sourceRepo);
    for (YumPackageReducedView obsoletePackage : obsoleteRPMs) {
      String[] strings = obsoletePackage.getLocation().getHref().split("/");
      GridFsFileDescriptor descriptor = new GridFsFileDescriptor(sourceRepo, strings[0], strings[1]);
      LOGGER.info("delete obsolete RPM {} obsoleted by a RPM in {}", descriptor.getPath(), targetRepo);

      gridFsService.delete(descriptor);
    }
  }


  private Set<YumPackageReducedView> filterRPMsFromPropagationChain(Filter filter, String targetRepo,
                                                                    String sourceRepo) {
    Map<String, Map<String, YumPackage>> newestTargetPackages = findNewestPackages(yumEntriesRepository.findByRepo(
      targetRepo));
    List<YumEntry> sourceRepoEntries = yumEntriesRepository.findByRepo(sourceRepo);
    return filterRPMs(filter, newestTargetPackages,
      sourceRepoEntries);
  }


  private Set<YumPackageReducedView> filterRPMs(Filter filter,
                                                Map<String, Map<String, YumPackage>> newestTargetPackagesByNameAndArch,
                                                List<YumEntry> sourceRepoEntries) {
    Set<YumPackageReducedView> result = new TreeSet<YumPackageReducedView>();
    for (YumEntry entry : sourceRepoEntries) {
      YumPackage yumPackage = entry.getYumPackage();
      YumPackage newestPackageInTargetRepo = getMatchingYumPackageByNameAndArchIfAny(newestTargetPackagesByNameAndArch,
        yumPackage);
      if (filter.select(newestPackageInTargetRepo, yumPackage)) {
        LOGGER.info(".. {}", filter.getFilterDescription());
        result.add(new YumPackageReducedView(yumPackage));
      }
    }
    return result;
  }


  private YumPackage getMatchingYumPackageByNameAndArchIfAny(Map<String, Map<String, YumPackage>> packagesByNameAndArch,
                                                             YumPackage yumPackage) {
    Map<String, YumPackage> rpmsByArch = packagesByNameAndArch.get(yumPackage.getName());
    if (rpmsByArch != null) {
      return rpmsByArch.get(yumPackage.getArch());
    }
    return null;
  }

  /**
  * determine newest RPMs by name and architecture
  * @param inputList list of yum entries in repo
  * @return a map of maps, first map key is rpm name, second maps key is arch
  */
  private Map<String, Map<String, YumPackage>> findNewestPackages(List<YumEntry> inputList) {
    Map<String, Map<String, YumPackage>> result = new HashMap<String, Map<String, YumPackage>>();
    for (YumEntry entry : inputList) {
      YumPackage yumPackage = entry.getYumPackage();

      Map<String, YumPackage> packageMap = result.get(yumPackage.getName());
      YumPackage packageForArchInMap = null;
      if (packageMap == null) {
        packageMap = new HashMap<String, YumPackage>();
        result.put(yumPackage.getName(), packageMap);
      } else {
        packageForArchInMap = packageMap.get(yumPackage.getArch());
      }
      if ((packageForArchInMap == null) ||
          (versionComparator.compare(yumPackage.getVersion(), packageForArchInMap.getVersion()) > 0)) {
        packageMap.put(yumPackage.getArch(), yumPackage);
      }
    }
    return result;
  }


  private interface Filter {
    boolean select(YumPackage newestTargetPackage, YumPackage sourcePackage);

    String getFilterDescription();
  }

  private class ObsoleteRpmFilter implements Filter {
    @Override
    public boolean select(YumPackage newestTargetPackage, YumPackage sourcePackage) {
      return (newestTargetPackage != null) &&
        (versionComparator.compare(newestTargetPackage.getVersion(), sourcePackage.getVersion()) > 0);
    }

    @Override
    public String getFilterDescription() {
      return "is obsolete";
    }
  }

  private class PropagatableRpmFilter implements Filter {
    @Override
    public boolean select(YumPackage newestTargetPackage, YumPackage sourcePackage) {
      return (newestTargetPackage == null) ||
        (versionComparator.compare(newestTargetPackage.getVersion(), sourcePackage.getVersion()) < 0);
    }

    @Override
    public String getFilterDescription() {
      return "is propagatable";
    }
  }

  private class DeleteObsoleteRPMsJob implements Runnable {
    public static final String JOB_CLASS = "job.class";
    public static final String JOB_START = "job.start";
    private final String sourceRepo;
    private final String propagationTargetRepo;

    public DeleteObsoleteRPMsJob(String initiatorDescription, String sourceRepo,
                                 String propagationTargetRepo) {
      this.sourceRepo = sourceRepo;
      this.propagationTargetRepo = propagationTargetRepo;
    }

    @Override
    public void run() {
      try {
        MDC.put(JOB_CLASS, this.getClass().getName());
        MDC.put(JOB_START, new Date());

        deleteObsoleteRPMs(propagationTargetRepo, sourceRepo);
      } finally {
        MDC.remove(JOB_CLASS);
        MDC.remove(JOB_START);
      }
    }
  }


  private class CheckForMissingFsFilesCallbackHandler implements DocumentCallbackHandler {
    private List<ObjectId> entriesWithMissingFile = new ArrayList<ObjectId>();

    @Override
    public void processDocument(DBObject dbObject) {
      ObjectId id = getId(dbObject);

      if (gridFsTemplate.findOne(Query.query(where("_id").is(id))) == null) {
        entriesWithMissingFile.add(id);
      }
    }

    private ObjectId getId(DBObject dbObject) {
      return ((ObjectId) dbObject.get("_id"));
    }

    private List<ObjectId> getEntriesWithMissingFile() {
      return entriesWithMissingFile;
    }
  }

  private class CheckForMissingEntriesCallbackHandler implements DocumentCallbackHandler {
    private List<ObjectId> filesWithMissingEntry = new ArrayList<ObjectId>();

    @Override
    public void processDocument(DBObject dbObject) {
      ObjectId id = getId(dbObject);

      if (yumEntriesRepository.findOne(id) == null) {
        filesWithMissingEntry.add(id);
      }
    }

    private ObjectId getId(DBObject dbObject) {
      return ((ObjectId) dbObject.get("_id"));
    }

    private List<ObjectId> getFilesWithMissingEntry() {
      return filesWithMissingEntry;
    }
  }
}
