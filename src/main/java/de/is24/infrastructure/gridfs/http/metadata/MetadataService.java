package de.is24.infrastructure.gridfs.http.metadata;

import com.mongodb.gridfs.GridFSDBFile;
import de.is24.infrastructure.gridfs.http.domain.YumEntry;
import de.is24.infrastructure.gridfs.http.gridfs.GridFsService;
import de.is24.infrastructure.gridfs.http.jaxb.Data;
import de.is24.infrastructure.gridfs.http.metadata.generation.DbGenerator;
import de.is24.infrastructure.gridfs.http.metadata.generation.FileListsGenerator;
import de.is24.infrastructure.gridfs.http.metadata.generation.OtherDbGenerator;
import de.is24.infrastructure.gridfs.http.metadata.generation.PrimaryDbGenerator;
import de.is24.infrastructure.gridfs.http.metadata.generation.RepoMdGenerator;
import de.is24.infrastructure.gridfs.http.monitoring.TimeMeasurement;
import de.is24.infrastructure.gridfs.http.repos.RepoCleaner;
import de.is24.infrastructure.gridfs.http.repos.RepoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import static java.io.File.createTempFile;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.time.DateUtils.addMinutes;


@ManagedResource
@Service
@TimeMeasurement
public class MetadataService {
  private static final Logger LOG = LoggerFactory.getLogger(MetadataService.class);
  private static final String METADATA_FILE_PATTERN = "/repodata/.*sqlite.bz2";

  private final GridFsService gridFs;
  private final RepoMdGenerator repoMdGenerator;
  private final YumEntriesRepository entriesRepository;
  private final RepoService repoService;
  private final RepoCleaner repoCleaner;
  private File tmpDir;
  private int outdatedMetaDataSurvivalTime;

  public MetadataService() {
    this.gridFs = null;
    this.entriesRepository = null;
    this.repoMdGenerator = null;
    this.repoService = null;
    this.repoCleaner = null;
  }

  @Autowired
  public MetadataService(GridFsService gridFs, YumEntriesRepository entriesRepository, RepoMdGenerator repoMdGenerator,
                         RepoService repoService, RepoCleaner repoCleaner) {
    this.gridFs = gridFs;
    this.entriesRepository = entriesRepository;
    this.repoMdGenerator = repoMdGenerator;
    this.repoService = repoService;
    this.repoCleaner = repoCleaner;
  }

  @ManagedOperation
  public void generateYumMetadata(String reponame) throws Exception {
    if (repoService.needsMetadataUpdate(reponame)) {
      repoCleaner.cleanup(reponame);
      doYumMetadataGenerationOnly(reponame);
    } else {
      LOG.debug("Generation for repository {} skipped. Because no update available.", reponame);
    }
  }

  @ManagedOperation
  public void doYumMetadataGenerationOnly(String reponame) throws Exception {
    LOG.info("Generating metadata for {} started ..", reponame);

    Date startTime = new Date();

    List<GridFSDBFile> filesToDelete = gridFs.findByFilenamePatternAndBeforeUploadDate(
      reponame + METADATA_FILE_PATTERN, createBeforeDate());
    List<YumEntry> entries = entriesRepository.findByRepo(reponame);
    List<Data> dbData = new ArrayList<Data>();
    for (DbGenerator dbGenerator : asList(new PrimaryDbGenerator(), new FileListsGenerator(), new OtherDbGenerator())) {
      LOG.info("Generate {}-DB for {}", dbGenerator.getName(), reponame);

      Data data = saveDb(dbGenerator, reponame, entries);
      data.setType(dbGenerator.getName() + "_db");
      dbData.add(data);
    }

    repoMdGenerator.generateRepoMdXml(reponame, dbData);
    gridFs.delete(filesToDelete);

    repoService.updateLastMetadataGeneration(reponame, startTime);

    LOG.info("Generating metadata for {} finished.", reponame);
  }

  private Date createBeforeDate() {
    return addMinutes(new Date(), -1 * outdatedMetaDataSurvivalTime);
  }

  private Data saveDb(DbGenerator dbGenerator, String reponame, List<YumEntry> entries) throws Exception {
    File tempDbFile = createTempFile(reponame + "-" + dbGenerator.getName(), ".sqlite", tmpDir);
    try {
      dbGenerator.createDb(tempDbFile, entries);

      Data data = gridFs.storeRepodataDbBz2(reponame, tempDbFile, dbGenerator.getName());
      data.setType(dbGenerator.getName() + "_db");
      return data;
    } finally {
      tempDbFile.delete();
    }
  }

  @Value("${metadata.tmp.dir:@null}")
  public void setTmpDir(File tmpDir) {
    this.tmpDir = tmpDir;
    if (tmpDir != null) {
      tmpDir.mkdirs();
    }
  }

  @Value("${metdata.outdated.survival.time:5}")
  public void setOutdatedMetaDataSurvivalTime(int outdatedMetaDataSurvivalTime) {
    this.outdatedMetaDataSurvivalTime = outdatedMetaDataSurvivalTime;
  }
}
