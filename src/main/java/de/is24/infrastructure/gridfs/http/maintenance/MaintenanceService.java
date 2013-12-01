package de.is24.infrastructure.gridfs.http.maintenance;

import de.is24.infrastructure.gridfs.http.domain.YumEntry;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackage;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageReducedView;
import de.is24.infrastructure.gridfs.http.gridfs.GridFsService;
import de.is24.infrastructure.gridfs.http.metadata.YumEntriesRepository;
import de.is24.infrastructure.gridfs.http.monitoring.TimeMeasurement;
import de.is24.infrastructure.gridfs.http.rpm.version.YumPackageVersionComparator;
import org.apache.log4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.join;


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

  /* for AOP autoproxying */
  protected MaintenanceService() {
  }


  @Autowired
  public MaintenanceService(TaskScheduler taskScheduler, YumEntriesRepository yumEntriesRepository,
                            GridFsService gridFsService) {
    this.taskScheduler = taskScheduler;
    this.yumEntriesRepository = yumEntriesRepository;
    this.gridFsService = gridFsService;
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

  private void deleteObsoleteRPMs(String targetRepo, String sourceRepo) {
    Set<YumPackageReducedView> obsoleteRPMs = filterRPMsFromPropagationChain(obsoleteRpmFiler, targetRepo, sourceRepo);
    for (YumPackageReducedView obsoletePackage : obsoleteRPMs) {
      String path = join(asList(sourceRepo, obsoletePackage.getLocation().getHref()), "/");
      LOGGER.info("delete obsolete RPM {} obsoleted by a RPM in {}", path, targetRepo);

      gridFsService.delete(path);
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


}
