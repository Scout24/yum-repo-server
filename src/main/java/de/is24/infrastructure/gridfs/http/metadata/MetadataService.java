package de.is24.infrastructure.gridfs.http.metadata;

import de.is24.infrastructure.gridfs.http.domain.RepoEntry;
import de.is24.infrastructure.gridfs.http.domain.RepoType;
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
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import static java.io.File.createTempFile;
import static java.util.Arrays.asList;
import static org.springframework.util.ObjectUtils.nullSafeEquals;


@ManagedResource
@Service
@TimeMeasurement
public class MetadataService {
  private static final Logger LOG = LoggerFactory.getLogger(MetadataService.class);
  private static final String METADATA_FILE_PATTERN = "/repodata/.*sqlite.bz2";

  private final GridFsService gridFsService;
  private final RepoMdGenerator repoMdGenerator;
  private final YumEntriesRepository entriesRepository;
  private final RepoService repoService;
  private final RepoCleaner repoCleaner;
  private final YumEntriesHashCalculator entriesHashCalculator;
  private File tmpDir;
  private int outdatedMetaDataSurvivalTime;

  @Autowired
  public MetadataService(GridFsService gridFs, YumEntriesRepository entriesRepository, RepoMdGenerator repoMdGenerator,
                         RepoService repoService, RepoCleaner repoCleaner,
                         YumEntriesHashCalculator entriesHashCalculator) {
    this.gridFsService = gridFs;
    this.entriesRepository = entriesRepository;
    this.repoMdGenerator = repoMdGenerator;
    this.repoService = repoService;
    this.repoCleaner = repoCleaner;
    this.entriesHashCalculator = entriesHashCalculator;
  }

  @ManagedOperation(description = "generate metadata if its necessary")
  public void generateYumMetadataIfNecessary(String reponame) throws IOException, SQLException {
    generateYumMetadata(reponame, false);
  }

  @ManagedOperation(description = "always generate and cleanup repo")
  public void generateYumMetadata(String reponame) throws IOException, SQLException {
    generateYumMetadata(reponame, true);
  }

  @ManagedOperation(description = "only generate metadata without cleanup, but always")
  public void doYumMetadataGenerationOnly(String reponame) throws IOException, SQLException {
    doYumMetadataGenerationOnlyInternal(reponame, entriesHashCalculator.hashForRepo(reponame));
  }

  private void generateYumMetadata(String reponame, boolean allwaysGenerate) throws IOException, SQLException {
    final RepoEntry repoEntry = repoService.ensureEntry(reponame, RepoType.STATIC, RepoType.SCHEDULED);
    final String calculatedHash = entriesHashCalculator.hashForRepo(repoEntry.getName());
    if (allwaysGenerate || needsMetadataUpdate(repoEntry, calculatedHash)) {
      repoCleaner.cleanup(reponame);
      doYumMetadataGenerationOnlyInternal(reponame, calculatedHash);
    } else {
      LOG.debug("Generation for repository {} skipped. Because no update available.", reponame);
    }
  }


  private boolean needsMetadataUpdate(final RepoEntry repoEntry, final String calculatedHash) {
    final String savedHash = repoEntry.getHashOfEntries();
    return !nullSafeEquals(calculatedHash, savedHash);
  }

  private void doYumMetadataGenerationOnlyInternal(final String reponame, final String calculatedHashOfEntries)
                                            throws IOException, SQLException {
    LOG.info("Generating metadata for {} started ..", reponame);

    gridFsService.markForDeletionByFilenameRegex(reponame + METADATA_FILE_PATTERN);

    Date startTime = new Date();

    List<YumEntry> entries = entriesRepository.findByRepo(reponame);
    List<Data> dbData = new ArrayList<Data>();
    for (DbGenerator dbGenerator : asList(new PrimaryDbGenerator(), new FileListsGenerator(), new OtherDbGenerator())) {
      LOG.info("Generate {}-DB for {}", dbGenerator.getName(), reponame);

      Data data = saveDb(dbGenerator, reponame, entries);
      data.setType(dbGenerator.getName() + "_db");
      dbData.add(data);
    }

    repoMdGenerator.generateRepoMdXml(reponame, dbData);

    repoService.updateLastMetadataGeneration(reponame, startTime, calculatedHashOfEntries);


    LOG.info("Generating metadata for {} finished.", reponame);
  }

  private Data saveDb(DbGenerator dbGenerator, String reponame, List<YumEntry> entries) throws IOException,
                                                                                               SQLException {
    File tempDbFile = createTempFile(reponame + "-" + dbGenerator.getName(), ".sqlite", tmpDir);
    try {
      dbGenerator.createDb(tempDbFile, entries);

      Data data = gridFsService.storeRepodataDbBz2(reponame, tempDbFile, dbGenerator.getName());
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
