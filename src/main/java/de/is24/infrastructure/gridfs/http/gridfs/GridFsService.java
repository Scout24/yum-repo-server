package de.is24.infrastructure.gridfs.http.gridfs;

import ch.lambdaj.function.compare.ArgumentComparator;
import com.google.common.annotations.VisibleForTesting;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSFile;
import com.mongodb.gridfs.GridFSInputFile;
import com.mongodb.gridfs.GridFSUtil;
import de.is24.infrastructure.gridfs.http.domain.YumEntry;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackage;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageChecksum;
import de.is24.infrastructure.gridfs.http.exception.BadRangeRequestException;
import de.is24.infrastructure.gridfs.http.exception.BadRequestException;
import de.is24.infrastructure.gridfs.http.exception.GridFSFileAlreadyExistsException;
import de.is24.infrastructure.gridfs.http.exception.GridFSFileNotFoundException;
import de.is24.infrastructure.gridfs.http.exception.InvalidRpmHeaderException;
import de.is24.infrastructure.gridfs.http.exception.RepositoryIsUndeletableException;
import de.is24.infrastructure.gridfs.http.jaxb.Data;
import de.is24.infrastructure.gridfs.http.metadata.YumEntriesRepository;
import de.is24.infrastructure.gridfs.http.repos.RepoService;
import de.is24.infrastructure.gridfs.http.rpm.RpmHeaderToYumPackageConverter;
import de.is24.infrastructure.gridfs.http.rpm.RpmHeaderWrapper;
import de.is24.infrastructure.gridfs.http.rpm.version.YumPackageVersionComparator;
import de.is24.util.monitoring.spring.TimeMeasurement;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.bson.types.ObjectId;
import org.freecompany.redline.ReadableChannelWrapper;
import org.freecompany.redline.Scanner;
import org.freecompany.redline.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.tx.MongoTx;
import org.springframework.http.MediaType;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static ch.lambdaj.Lambda.on;
import static com.mongodb.gridfs.GridFSUtil.mergeMetaData;
import static com.mongodb.gridfs.GridFSUtil.remove;
import static de.is24.infrastructure.gridfs.http.domain.RepoType.SCHEDULED;
import static de.is24.infrastructure.gridfs.http.domain.RepoType.STATIC;
import static de.is24.infrastructure.gridfs.http.metadata.generation.DbGenerator.DB_VERSION;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.ARCH_KEY;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.ARCH_KEY_REPO_DATA;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.FILENAME_KEY;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.GRIDFS_FILES_COLLECTION;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.MARKED_AS_DELETED_KEY;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.METADATA_ARCH_KEY;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.METADATA_MARKED_AS_DELETED_KEY;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.METADATA_REPO_KEY;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.METADATA_UPLOAD_DATE_KEY;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.REPO_KEY;
import static de.is24.infrastructure.gridfs.http.mongo.ObjectIdCriteria.whereObjectIdIs;
import static de.is24.infrastructure.gridfs.http.repos.RepositoryNameValidator.validateRepoName;
import static de.is24.infrastructure.gridfs.http.security.Permission.PROPAGATE_FILE;
import static de.is24.infrastructure.gridfs.http.security.Permission.PROPAGATE_REPO;
import static de.is24.infrastructure.gridfs.http.security.Permission.READ_FILE;
import static java.lang.String.format;
import static java.nio.channels.Channels.newChannel;
import static java.util.Collections.sort;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.apache.commons.codec.digest.DigestUtils.getSha256Digest;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;
import static org.apache.commons.lang.StringUtils.countMatches;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.core.query.Update.update;
import static org.springframework.data.mongodb.gridfs.GridFsCriteria.whereFilename;
import static org.springframework.data.mongodb.gridfs.GridFsCriteria.whereMetaData;


@ManagedResource
@Service
public class GridFsService {
  private static final int MB = 1024 * 1024;
  private static final int FIVE_MB = 5 * MB;
  private static final String SHA256_KEY = "sha256";
  private static final String OPEN_SIZE_KEY = "open_size";
  private static final Logger LOGGER = LoggerFactory.getLogger(GridFsService.class);
  private static final int BUFFER_SIZE = 16 * 1024 * 1024;
  private static final String BZ2_CONTENT_TYPE = new MediaType("application", "x-bzip2").toString();
  private static final String OPEN_SHA256_KEY = "open_sha256";
  private static final String ENDS_WITH_RPM_REGEX = ".*\\.rpm$";
  public static final String CONTENT_TYPE_APPLICATION_X_RPM = "application/x-rpm";
  public static final String HAS_DESCRIPTOR_READ_PERMISSION = "hasPermission(#descriptor, '" + READ_FILE + "')";

  private final GridFS gridFs;
  private final GridFsOperations gridFsTemplate;
  private final MongoTemplate mongoTemplate;
  private final YumEntriesRepository yumEntriesRepository;
  private final RepoService repoService;
  private YumPackageVersionComparator comparator = new YumPackageVersionComparator();

  //needed for cglib proxy
  public GridFsService() {
    this.gridFsTemplate = null;
    this.mongoTemplate = null;
    this.yumEntriesRepository = null;
    this.gridFs = null;
    this.repoService = null;
  }

  @Autowired
  public GridFsService(GridFS gridFs, GridFsOperations gridFsTemplate, MongoTemplate mongoTemplate,
                       YumEntriesRepository yumEntriesRepository, RepoService repoService) {
    this.gridFs = gridFs;
    this.gridFsTemplate = gridFsTemplate;
    this.mongoTemplate = mongoTemplate;
    this.yumEntriesRepository = yumEntriesRepository;
    this.repoService = repoService;

    setupIndices();
  }

  private void setupIndices() {
    DBCollection filesCollection = mongoTemplate.getCollection(GRIDFS_FILES_COLLECTION);
    filesCollection.ensureIndex(METADATA_REPO_KEY);
    filesCollection.ensureIndex(METADATA_ARCH_KEY);
    filesCollection.ensureIndex(METADATA_UPLOAD_DATE_KEY);
    filesCollection.ensureIndex(METADATA_MARKED_AS_DELETED_KEY);
  }

  @TimeMeasurement
  @PreAuthorize("hasPermission(#sourceFile, '" + PROPAGATE_FILE + "')")
  public GridFsFileDescriptor propagateRpm(String sourceFile, String destinationRepo) {
    validatePathToRpm(sourceFile);

    GridFsFileDescriptor descriptor = new GridFsFileDescriptor(sourceFile);
    validateDestinationRepo(descriptor.getRepo(), destinationRepo);

    GridFSDBFile dbFile = findDbFileByPathDirectlyOrFindNewestRpmMatchingNameAndArch(descriptor);
    if (dbFile == null) {
      throw new GridFSFileNotFoundException("Could not find file.", sourceFile);
    }
    if (isMarkedForDeletion(dbFile)) {
      throw new GridFSFileNotFoundException("File is marked for deletion.", sourceFile);
    }

    String sourceRepo = (String) dbFile.getMetaData().get(REPO_KEY);
    GridFsFileDescriptor fileDescriptor = move(dbFile, destinationRepo);
    repoService.createOrUpdate(sourceRepo);
    repoService.createOrUpdate(destinationRepo);
    return fileDescriptor;
  }


  @TimeMeasurement
  @PreAuthorize(HAS_DESCRIPTOR_READ_PERMISSION)
  public GridFSDBFile findFileByDescriptor(GridFsFileDescriptor descriptor) {
    return internalUnsecuredFindFileByDescriptor(descriptor);
  }

  @TimeMeasurement
  public GridFSDBFile internalUnsecuredFindFileByDescriptor(GridFsFileDescriptor descriptor) {
    return gridFsTemplate.findOne(query(whereFilename().is(descriptor.getPath())));
  }


  @TimeMeasurement
  @PreAuthorize(HAS_DESCRIPTOR_READ_PERMISSION)
  public GridFSDBFile getFileByDescriptor(GridFsFileDescriptor descriptor) {
    GridFSDBFile dbFile = findFileByDescriptor(descriptor);
    if (dbFile == null) {
      throw new GridFSFileNotFoundException("Could not find file in gridfs.", descriptor.getPath());
    }
    return dbFile;
  }

  @TimeMeasurement
  public void delete(GridFsFileDescriptor descriptor) {
    GridFSDBFile dbFile = getFileByDescriptor(descriptor);
    delete(dbFile);

    String sourceRepo = (String) dbFile.getMetaData().get(REPO_KEY);
    if (sourceRepo != null) {
      repoService.createOrUpdate(sourceRepo);
    }
  }

  @TimeMeasurement
  public void delete(List<GridFSDBFile> dbFiles) {
    for (GridFSDBFile dbFile : dbFiles) {
      delete(dbFile);
    }
  }

  public boolean isMarkedForDeletion(GridFSDBFile dbFile) {
    return dbFile.getMetaData().get(MARKED_AS_DELETED_KEY) != null;
  }

  public void markForDeletionById(ObjectId id) {
    markForDeletion(whereObjectIdIs(id));
  }

  public void markForDeletionByPath(final String path) {
    markForDeletion(whereFilename().is(path));
  }

  public void markForDeletionByFilenameRegex(final String regex) {
    markForDeletion(whereFilename().regex(regex));
  }

  private void markForDeletion(final Criteria criteria) {
    mongoTemplate.updateMulti(query(criteria.and(METADATA_MARKED_AS_DELETED_KEY).is(null)),
      update(METADATA_MARKED_AS_DELETED_KEY, new Date()),
      GRIDFS_FILES_COLLECTION);
  }

  @ManagedOperation
  public List<String> listFilesMarkedAsDeleted() {
    final List<GridFSDBFile> gridFSDBFiles = gridFsTemplate.find(query(whereMetaData(MARKED_AS_DELETED_KEY).ne(null)));
    final List<String> filenames = new ArrayList<>(gridFSDBFiles.size());
    for (GridFSDBFile file : gridFSDBFiles) {
      filenames.add(file.getFilename() + " " + file.getMetaData().get(MARKED_AS_DELETED_KEY).toString());
    }
    return filenames;
  }

  @ManagedOperation
  @MongoTx(writeConcern = "FSYNCED")
  public void removeFilesMarkedAsDeletedBefore(final Date before) {
    LOGGER.info("removing files marked as deleted before {}", before);

    final List<GridFSDBFile> filesToDelete = gridFsTemplate.find(query(
      whereMetaData(MARKED_AS_DELETED_KEY).lt(before)));

    int counter = 0;
    for (GridFSDBFile file : filesToDelete) {
      final long lengthInBytes = file.getLength();
      final String filename = file.getFilename();
      LOGGER.info("removing file {}", filename);
      GridFSUtil.remove(file);

      //wait depending on the size/count of deleted file to let the mongo cluster do the sync without 'dieing' on io wait
      if (lengthInBytes > FIVE_MB) {
        waitAfterDeleteOfLargeFile(lengthInBytes, filename);

      }
      if (counter > 100) {
        LOGGER.info("waiting 2000 ms after removal of 100 files");
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
          //should only happen on server shutdown
        }
        counter = 0;
      } else {
        counter++;
      }
    }

    LOGGER.info("finished removing files marked as deleted before {}", before);
  }

  private GridFSDBFile findDbFileByPathDirectlyOrFindNewestRpmMatchingNameAndArch(GridFsFileDescriptor descriptor) {
    GridFSDBFile dbFile = findFileByDescriptor(descriptor);
    if (dbFile != null) {
      if (!dbFile.getFilename().endsWith(".rpm")) {
        throw new BadRequestException("Source rpm must end with .rpm!");
      }
      return dbFile;
    }

    return findNewestRpmByPath(descriptor);
  }

  private GridFSDBFile findNewestRpmByPath(GridFsFileDescriptor descriptor) {
    return findNewestRpmInRepoByNameAndArch(descriptor.getRepo(), descriptor.getArch(), descriptor.getFilename());
  }

  private GridFSDBFile findNewestRpmInRepoByNameAndArch(String repo, String arch, String name) {
    List<YumEntry> entries = yumEntriesRepository.findByRepoAndYumPackageArchAndYumPackageName(repo, arch, name);
    if (entries.isEmpty()) {
      return null;
    }

    sort(entries, new ArgumentComparator<>(on(YumEntry.class).getYumPackage().getVersion(), comparator));

    YumEntry lastEntry = entries.get(entries.size() - 1);
    return gridFs.find(lastEntry.getId());
  }

  private GridFsFileDescriptor move(GridFSDBFile dbFile, String destinationRepo) {
    YumEntry yumEntry = yumEntriesRepository.findOne((ObjectId) dbFile.getId());
    yumEntry.setRepo(null);
    yumEntriesRepository.save(yumEntry);

    GridFsFileDescriptor descriptor = new GridFsFileDescriptor(dbFile);
    descriptor.setRepo(destinationRepo);

    GridFSDBFile dbToOverrideFile = findFileByDescriptor(descriptor);
    dbFile.put(FILENAME_KEY, descriptor.getPath());

    dbFile.getMetaData().put(REPO_KEY, destinationRepo);
    dbFile.save();

    if (dbToOverrideFile != null) {
      delete(dbToOverrideFile);
    }

    yumEntry.setRepo(destinationRepo);
    yumEntriesRepository.save(yumEntry);

    return descriptor;
  }

  private void waitAfterDeleteOfLargeFile(long lengthInBytes, String filename) {
    //24MB 1600ms
    //600MB 40sec
    final long lengthInMb = lengthInBytes / MB;
    final long millisToWait = (long) ((lengthInMb / 60f) * 4000);
    LOGGER.info("waiting {}ms after remove of large file {}({}MB)", millisToWait, filename, lengthInMb);
    try {
      Thread.sleep(millisToWait);
    } catch (InterruptedException e) {
      //should only happen on server shutdown
    }
  }

  @PreAuthorize(HAS_DESCRIPTOR_READ_PERMISSION)
  public BoundedGridFsResource getResource(GridFsFileDescriptor descriptor) throws IOException {
    return getResource(descriptor, 0);
  }

  @PreAuthorize(HAS_DESCRIPTOR_READ_PERMISSION)
  public BoundedGridFsResource getResource(GridFsFileDescriptor descriptor, long startPos) throws IOException {
    return new BoundedGridFsResource(getGridFSDBFileCheckedStartPosIsValid(descriptor, startPos), startPos);
  }

  @PreAuthorize(HAS_DESCRIPTOR_READ_PERMISSION)
  public BoundedGridFsResource getResource(GridFsFileDescriptor descriptor, long startPos, long size)
                                    throws IOException {
    return new BoundedGridFsResource(getGridFSDBFileCheckedStartPosIsValid(descriptor, startPos), startPos, size);
  }

  private GridFSDBFile getGridFSDBFileCheckedStartPosIsValid(GridFsFileDescriptor descriptor, long startPos) {
    GridFSDBFile gridFSDBFile = getFileByDescriptor(descriptor);
    if (startPos >= gridFSDBFile.getLength()) {
      throw new BadRangeRequestException(format(
          "Range start is bigger than file size.\n" +
          "\tpath: %s\n" +
          "\tstartPos: %s\n" +
          "\tlength: %s\n", descriptor.getPath(), startPos, gridFSDBFile.getLength()));
    }
    return gridFSDBFile;
  }

  @PreAuthorize("hasPermission(#sourceRepo, '" + PROPAGATE_REPO + "')")
  public void propagateRepository(String sourceRepo, String destinationRepo) {
    validateRepoName(sourceRepo);
    validateDestinationRepo(sourceRepo, destinationRepo);

    List<GridFSDBFile> sourceRpms = gridFsTemplate.find(query(
      whereMetaData(REPO_KEY).is(sourceRepo)
      .and(METADATA_MARKED_AS_DELETED_KEY)
      .is(null)
      .andOperator(whereFilename().regex(".*\\.rpm$"))));
    for (GridFSDBFile dbFile : sourceRpms) {
      if (!isMarkedForDeletion(dbFile)) {
        move(dbFile, destinationRepo);
      }
    }

    repoService.createOrUpdate(sourceRepo);
    repoService.createOrUpdate(destinationRepo);
  }

  @TimeMeasurement
  public void storeRpm(String reponame, InputStream inputStream) throws InvalidRpmHeaderException, IOException {
    validateRepoName(reponame);

    BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream, BUFFER_SIZE);
    bufferedInputStream.mark(BUFFER_SIZE);

    YumPackage yumPackage = convertHeader(bufferedInputStream);
    bufferedInputStream.reset();

    GridFsFileDescriptor descriptor = new GridFsFileDescriptor(reponame, yumPackage);
    final GridFSFile dbFile = storeFileWithMetaInfo(bufferedInputStream, descriptor);

    yumEntriesRepository.save(createYumEntry(yumPackage, dbFile));
    repoService.createOrUpdate(reponame);
    LOGGER.info("Stored RPM {}/{}", reponame, yumPackage.getLocation().getHref());
  }

  @TimeMeasurement
  public Data storeRepodataDbBz2(String reponame, File dbFile, String name) throws IOException {
    validateRepoName(reponame);

    GridFsFileDescriptor descriptor = new GridFsFileDescriptor(reponame, ARCH_KEY_REPO_DATA, name);
    GridFSInputFile inputFile = gridFs.createFile();
    inputFile.setContentType(BZ2_CONTENT_TYPE);

    InputStream fileInputStream = new BufferedInputStream(new FileInputStream(dbFile));
    DigestInputStream uncompressedDigestInputStream = new DigestInputStream(fileInputStream, getSha256Digest());
    DigestOutputStream compressedDigestOutputStream;
    try {
      OutputStream gridFsOutputStream = inputFile.getOutputStream();
      compressedDigestOutputStream = new DigestOutputStream(gridFsOutputStream, getSha256Digest());

      BZip2CompressorOutputStream bzip2OutputStream = new BZip2CompressorOutputStream(compressedDigestOutputStream);

      copy(uncompressedDigestInputStream, bzip2OutputStream);
      bzip2OutputStream.close();
    } finally {
      uncompressedDigestInputStream.close();
    }

    String checksum = encodeHexString(compressedDigestOutputStream.getMessageDigest().digest());
    String openChecksum = encodeHexString(uncompressedDigestInputStream.getMessageDigest().digest());
    Data data = createRepoMdData(name, dbFile, openChecksum, checksum, inputFile);
    String finalFilename = reponame + "/" + data.getLocation().getHref();

    gridFs.remove(finalFilename);

    DBObject metaData = createBasicMetaDataObject(descriptor,
      data.getChecksum().getChecksum());
    metaData.put(OPEN_SHA256_KEY, data.getOpenChecksum().getChecksum());
    metaData.put(OPEN_SIZE_KEY, dbFile.length());

    inputFile.setMetaData(metaData);
    inputFile.put(FILENAME_KEY, finalFilename);
    inputFile.getOutputStream().close();

    return data;
  }

  @TimeMeasurement
  public void deleteRepository(String reponame) {
    validateRepoName(reponame);

    if (repoService.ensureEntry(reponame, STATIC, SCHEDULED).isUndeletable()) {
      throw new RepositoryIsUndeletableException(reponame);
    }

    mongoTemplate.remove(query(where(REPO_KEY).is(reponame)), YumEntry.class);

    markForDeletion(whereMetaData(REPO_KEY).is(reponame));

    repoService.delete(reponame);
  }

  @ManagedOperation
  public void regenerateMetadataFor(GridFsFileDescriptor descriptor) throws InvalidRpmHeaderException {
    regenerateMetadataFor(getFileByDescriptor(descriptor));
  }

  @ManagedOperation
  public void regenerateMetadataForAllFiles() throws InvalidRpmHeaderException {
    for (GridFSDBFile dbFile : gridFs.find(query(whereFilename().regex(ENDS_WITH_RPM_REGEX)).getQueryObject())) {
      regenerateMetadataFor(dbFile);
    }
  }

  @VisibleForTesting
  GridFSFile storeFileWithMetaInfo(InputStream inputStream,
                                   GridFsFileDescriptor descriptor) throws IOException {
    GridFSDBFile existingDbFile = internalUnsecuredFindFileByDescriptor(descriptor);
    if (existingDbFile != null) {
      throw new GridFSFileAlreadyExistsException("Reupload of rpm is not possible.", descriptor.getPath());
    }

    DigestInputStream digestInputStream = new DigestInputStream(inputStream, getSha256Digest());
    GridFSFile inputFile = gridFsTemplate.store(digestInputStream, descriptor.getPath(),
      CONTENT_TYPE_APPLICATION_X_RPM);
    closeQuietly(digestInputStream);

    String sha256Hash = encodeHexString(digestInputStream.getMessageDigest().digest());
    DBObject metaData = createBasicMetaDataObject(descriptor, sha256Hash);
    mergeMetaData(inputFile, metaData);

    inputFile.save();

    return inputFile;
  }

  private void validatePathToRpm(String path) {
    if (isBlank(path)) {
      throw new BadRequestException("Source rpm is not allowed to be blank!");
    }
    if (countMatches(path, "/") != 2) {
      throw new BadRequestException("Rpm file has invalid depth!");
    }
  }

  private void validateDestinationRepo(String sourceRepo, String destinationRepo) {
    validateRepoName(destinationRepo);
    if (destinationRepo.equals(sourceRepo)) {
      throw new BadRequestException("Destination repo have not to be equals to source repo: " + sourceRepo);
    }
  }

  private void delete(GridFSDBFile dbFile) {
    yumEntriesRepository.delete((ObjectId) dbFile.getId());
    remove(dbFile);
  }

  private DBObject createBasicMetaDataObject(GridFsFileDescriptor descriptor, String sha256Hash) {
    DBObject metaData = new BasicDBObject();
    metaData.put(REPO_KEY, descriptor.getRepo());
    metaData.put(ARCH_KEY, descriptor.getArch());
    metaData.put(SHA256_KEY, sha256Hash);
    return metaData;
  }

  private YumPackage convertHeader(InputStream inputStream) throws InvalidRpmHeaderException {
    RpmHeaderWrapper headerWrapper = new RpmHeaderWrapper(readHeader(inputStream));
    RpmHeaderToYumPackageConverter converter = new RpmHeaderToYumPackageConverter(headerWrapper);
    return converter.convert();
  }

  private YumEntry createYumEntry(YumPackage yumPackage, GridFSFile inputFile) {
    yumPackage.getSize().setPackaged((int) inputFile.getLength());
    yumPackage.setChecksum(new YumPackageChecksum("sha256", inputFile.getMetaData().get(SHA256_KEY).toString()));
    yumPackage.getTime().setFile(yumTime(inputFile.getUploadDate()));
    return new YumEntry((ObjectId) inputFile.getId(), inputFile.getMetaData().get(REPO_KEY).toString(), yumPackage);
  }

  private Data createRepoMdData(String name, File dbFile, String openChecksum, String checksum,
                                GridFSFile gridFsFile) {
    Data data = new Data();
    data.setChecksum(SHA256_KEY, checksum);
    data.setSize(gridFsFile.getLength());
    data.setOpenSize(dbFile.length());
    data.setOpenChecksum(SHA256_KEY, openChecksum);
    data.setLocation(createRepoMdLocation(name, data.getChecksum().getChecksum()));
    data.setTimestamp((int) (gridFsFile.getUploadDate().getTime() / 1000));
    data.setDatabaseVersion(DB_VERSION);
    return data;
  }

  private String createRepoMdLocation(String name, String checksum) {
    return ARCH_KEY_REPO_DATA + "/" + name + "-" + checksum + ".sqlite.bz2";
  }

  private static int yumTime(Date date) {
    return (int) (date.getTime() / 1000);
  }

  private Header readHeader(InputStream inputStream) throws InvalidRpmHeaderException {
    ReadableChannelWrapper channel = new ReadableChannelWrapper(newChannel(inputStream));
    try {
      return new Scanner().run(channel).getHeader();
    } catch (Exception e) {
      throw new InvalidRpmHeaderException("Could not read rpm header.", e);
    }
  }

  private void regenerateMetadataFor(GridFSDBFile dbFile) throws InvalidRpmHeaderException {
    LOGGER.info("regenerating metadata for {}", dbFile.getFilename());
    try {
      YumPackage yumPackage = convertHeader(dbFile.getInputStream());
      YumEntry yumEntry = createYumEntry(yumPackage, dbFile);
      yumEntriesRepository.save(yumEntry);
    } catch (InvalidRpmHeaderException e) {
      LOGGER.error("Generating metadata for " + dbFile.getFilename() + " failed.", e);
      throw e;
    }
  }
}
