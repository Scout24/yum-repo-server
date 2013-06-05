package de.is24.infrastructure.gridfs.http.gridfs;

import ch.lambdaj.function.compare.ArgumentComparator;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSFile;
import com.mongodb.gridfs.GridFSInputFile;
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
import de.is24.infrastructure.gridfs.http.monitoring.TimeMeasurement;
import de.is24.infrastructure.gridfs.http.repos.RepoService;
import de.is24.infrastructure.gridfs.http.rpm.RpmHeaderToYumPackageConverter;
import de.is24.infrastructure.gridfs.http.rpm.RpmHeaderWrapper;
import de.is24.infrastructure.gridfs.http.rpm.version.YumPackageVersionComparator;
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
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.MediaType;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
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
import static com.mongodb.gridfs.GridFS.DEFAULT_CHUNKSIZE;
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
  private static final String SHA256_KEY = "sha256";
  private static final String OPEN_SIZE_KEY = "open_size";
  private static final Logger LOGGER = LoggerFactory.getLogger(GridFsService.class);
  private static final int BUFFER_SIZE = 16 * 1024 * 1024;
  private static final String BZ2_CONTENT_TYPE = new MediaType("application", "x-bzip2").toString();
  private static final String OPEN_SHA256_KEY = "open_sha256";
  private static final String ENDS_WITH_RPM_REGEX = ".*\\.rpm$";

  private final GridFS gridFs;
  private final GridFsTemplate gridFsTemplate;
  private final MongoTemplate mongoTemplate;
  private final YumEntriesRepository yumEntriesRepository;
  private final RepoService repoService;
  private int chunkSize = DEFAULT_CHUNKSIZE;
  private YumPackageVersionComparator comparator = new YumPackageVersionComparator();

  public GridFsService() {
    this.gridFsTemplate = null;
    this.mongoTemplate = null;
    this.yumEntriesRepository = null;
    this.gridFs = null;
    this.repoService = null;
  }

  @Autowired
  public GridFsService(GridFS gridFs, GridFsTemplate gridFsTemplate, MongoTemplate mongoTemplate,
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

  public String propagateRpm(String sourceFile, String destinationRepo) {
    validatePathToRpm(sourceFile);
    validateRepoName(destinationRepo);

    GridFSDBFile dbFile = findDbFileByPathDirectlyOrFindNewestRpmMatchingNameAndArch(sourceFile);
    if (dbFile == null) {
      throw new GridFSFileNotFoundException("Could not find file.", sourceFile);
    }

    String sourceRepo = (String) dbFile.getMetaData().get(REPO_KEY);
    String destinationPath = move(dbFile, destinationRepo);
    repoService.createOrUpdate(sourceRepo);
    repoService.createOrUpdate(destinationRepo);
    return destinationPath;
  }

  private GridFSDBFile findDbFileByPathDirectlyOrFindNewestRpmMatchingNameAndArch(String sourceFile) {
    GridFSDBFile dbFile = findFileByPath(sourceFile);
    if (dbFile != null) {
      if (!sourceFile.endsWith(".rpm")) {
        throw new BadRequestException("Source rpm must end with .rpm!");
      }
      return dbFile;
    }

    return findNewestRpmByPath(sourceFile);
  }

  private GridFSDBFile findNewestRpmByPath(String path) {
    String[] parts = path.split("/");
    if (parts.length != 3) {
      throw new IllegalArgumentException("This is not a valid path to an rpm file in a repository: " + path);
    }

    String repo = parts[0];
    String arch = parts[1];
    String name = parts[2];
    return findNewestRpmInRepoByNameAndArch(repo, arch, name);
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

  private String move(GridFSDBFile dbFile, String destinationRepo) {
    YumEntry yumEntry = yumEntriesRepository.findOne((ObjectId) dbFile.getId());
    yumEntry.setRepo(null);
    yumEntriesRepository.save(yumEntry);

    String destinationPath = destinationRepo + dbFile.getFilename().substring(dbFile.getFilename().indexOf("/"));
    GridFSDBFile dbToOverrideFile = findFileByPath(destinationPath);
    dbFile.put(FILENAME_KEY, destinationPath);

    dbFile.getMetaData().put(REPO_KEY, destinationRepo);
    dbFile.save();

    if (dbToOverrideFile != null) {
      delete(dbToOverrideFile);
    }

    yumEntry.setRepo(destinationRepo);
    yumEntriesRepository.save(yumEntry);

    return destinationPath;
  }

  public GridFSDBFile findFileByPath(String path) {
    return gridFsTemplate.findOne(query(whereFilename().is(path)));
  }

  @TimeMeasurement
  public List<GridFSDBFile> findByFilenamePattern(String regex) {
    return gridFsTemplate.find(query(whereFilename().regex(regex)));
  }

  @TimeMeasurement
  public GridFSDBFile getFileByPath(String path) {
    GridFSDBFile dbFile = findFileByPath(path);
    if (dbFile == null) {
      throw new GridFSFileNotFoundException("Could not find file in gridfs.", path);
    }
    return dbFile;
  }

  @TimeMeasurement
  public void delete(String path) {
    GridFSDBFile dbFile = getFileByPath(path);
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
      filenames.add(file.getFilename());
    }
    return filenames;
  }

  @ManagedOperation
  public void removeFilesMarkedAsDeletedBefore(final Date before) {
    LOGGER.info("removing files marked as deleted before {}", before);

    gridFsTemplate.delete(query(
        whereMetaData(MARKED_AS_DELETED_KEY).lt(before)));
  }

  public BoundedGridFsResource getResource(String path) throws IOException {
    return getResource(path, 0);
  }

  public BoundedGridFsResource getResource(String path, long startPos) throws IOException {
    return new BoundedGridFsResource(getGridFSDBFileCheckedStartPosIsValid(path, startPos), startPos);
  }

  public BoundedGridFsResource getResource(String path, long startPos, long size) throws IOException {
    return new BoundedGridFsResource(getGridFSDBFileCheckedStartPosIsValid(path, startPos), startPos, size);
  }

  private GridFSDBFile getGridFSDBFileCheckedStartPosIsValid(String path, long startPos) {
    GridFSDBFile gridFSDBFile = getFileByPath(path);
    if (startPos >= gridFSDBFile.getLength()) {
      throw new BadRangeRequestException(format(
          "Range start is bigger than file size.\n" +
          "\tpath: %s\n" +
          "\tstartPos: %s\n" +
          "\tlength: %s\n", path, startPos, gridFSDBFile.getLength()));
    }
    return gridFSDBFile;
  }

  public void propagateRepository(String sourceRepo, String destinationRepo) {
    validateRepoName(sourceRepo);
    validateRepoName(destinationRepo);

    List<GridFSDBFile> sourceRpms = gridFsTemplate.find(query(
      whereMetaData(REPO_KEY).is(sourceRepo).andOperator(whereFilename().regex(".*\\.rpm$"))));
    for (GridFSDBFile dbFile : sourceRpms) {
      move(dbFile, destinationRepo);
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

    GridFSInputFile dbFile = storeFileWithMetaInfo(bufferedInputStream, reponame, yumPackage.getArch(),
      yumPackage.getLocation().getHref());

    yumEntriesRepository.save(createYumEntry(yumPackage, dbFile));
    repoService.createOrUpdate(reponame);
    LOGGER.info("Stored RPM {}/{}", reponame, yumPackage.getLocation().getHref());
  }

  @TimeMeasurement
  public Data storeRepodataDbBz2(String reponame, File dbFile, String name) throws IOException {
    validateRepoName(reponame);

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

    DBObject metaData = createBasicMetaDataObject(reponame, ARCH_KEY_REPO_DATA,
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

  @ManagedAttribute
  public int getChunkSize() {
    return chunkSize;
  }

  @ManagedAttribute
  public void setChunkSize(int chunkSize) {
    this.chunkSize = chunkSize;
  }

  @ManagedOperation
  public void regenerateMetadataFor(String path) throws InvalidRpmHeaderException {
    regenerateMetadataFor(getFileByPath(path));
  }

  @ManagedOperation
  public void regenerateMetadataForAllFiles() throws InvalidRpmHeaderException {
    for (GridFSDBFile dbFile : gridFs.find(query(whereFilename().regex(ENDS_WITH_RPM_REGEX)).getQueryObject())) {
      regenerateMetadataFor(dbFile);
    }
  }

  private GridFSInputFile storeFileWithMetaInfo(InputStream inputStream,
                                                String reponame, String arch, String pathInRepo)
                                         throws InvalidRpmHeaderException, IOException {
    String rpmPath = reponame + "/" + pathInRepo;
    GridFSDBFile existingDbFile = findFileByPath(rpmPath);
    if (existingDbFile != null) {
      throw new GridFSFileAlreadyExistsException("Reupload of rpm is not possible.", rpmPath);
    }

    DigestInputStream digestInputStream = new DigestInputStream(inputStream, getSha256Digest());
    GridFSInputFile inputFile = (GridFSInputFile) gridFsTemplate.store(digestInputStream, rpmPath);
    inputFile.setContentType("application/x-rpm");
    inputFile.setChunkSize(chunkSize);
    inputFile.save();
    closeQuietly(digestInputStream);

    String sha256Hash = encodeHexString(digestInputStream.getMessageDigest().digest());
    DBObject metaData = createBasicMetaDataObject(reponame, arch, sha256Hash);
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

  private void delete(GridFSDBFile dbFile) {
    yumEntriesRepository.delete((ObjectId) dbFile.getId());
    remove(dbFile);
  }

  private DBObject createBasicMetaDataObject(String reponame, String arch, String sha256Hash) {
    DBObject metaData = new BasicDBObject();
    metaData.put(REPO_KEY, reponame);
    metaData.put(ARCH_KEY, arch);
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
