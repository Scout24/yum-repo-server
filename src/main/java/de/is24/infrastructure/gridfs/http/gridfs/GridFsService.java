package de.is24.infrastructure.gridfs.http.gridfs;

import ch.lambdaj.function.compare.ArgumentComparator;
import com.mongodb.DBCollection;
import de.is24.infrastructure.gridfs.http.domain.YumEntry;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackage;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageChecksum;
import de.is24.infrastructure.gridfs.http.exception.BadRangeRequestException;
import de.is24.infrastructure.gridfs.http.exception.BadRequestException;
import de.is24.infrastructure.gridfs.http.exception.GridFSFileNotFoundException;
import de.is24.infrastructure.gridfs.http.exception.InvalidRpmHeaderException;
import de.is24.infrastructure.gridfs.http.exception.RepositoryIsUndeletableException;
import de.is24.infrastructure.gridfs.http.jaxb.Data;
import de.is24.infrastructure.gridfs.http.metadata.YumEntriesRepository;
import de.is24.infrastructure.gridfs.http.repos.RepoService;
import de.is24.infrastructure.gridfs.http.rpm.RpmHeaderToYumPackageConverter;
import de.is24.infrastructure.gridfs.http.rpm.RpmHeaderWrapper;
import de.is24.infrastructure.gridfs.http.rpm.version.YumPackageVersionComparator;
import de.is24.infrastructure.gridfs.http.storage.FileStorageItem;
import de.is24.infrastructure.gridfs.http.storage.FileStorageService;
import de.is24.infrastructure.gridfs.http.storage.UploadResult;
import de.is24.util.monitoring.spring.TimeMeasurement;
import org.bson.types.ObjectId;
import org.freecompany.redline.ReadableChannelWrapper;
import org.freecompany.redline.Scanner;
import org.freecompany.redline.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import static ch.lambdaj.Lambda.on;
import static de.is24.infrastructure.gridfs.http.domain.RepoType.SCHEDULED;
import static de.is24.infrastructure.gridfs.http.domain.RepoType.STATIC;
import static de.is24.infrastructure.gridfs.http.metadata.generation.DbGenerator.DB_VERSION;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.GRIDFS_FILES_COLLECTION;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.METADATA_ARCH_KEY;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.METADATA_MARKED_AS_DELETED_KEY;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.METADATA_REPO_KEY;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.METADATA_UPLOAD_DATE_KEY;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.REPO_KEY;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.SHA256_KEY;
import static de.is24.infrastructure.gridfs.http.mongo.ObjectIdCriteria.whereObjectIdIs;
import static de.is24.infrastructure.gridfs.http.repos.RepositoryNameValidator.validateRepoName;
import static de.is24.infrastructure.gridfs.http.security.Permission.PROPAGATE_FILE;
import static de.is24.infrastructure.gridfs.http.security.Permission.PROPAGATE_REPO;
import static de.is24.infrastructure.gridfs.http.security.Permission.READ_FILE;
import static java.lang.String.format;
import static java.nio.channels.Channels.newChannel;
import static java.util.Collections.sort;
import static org.apache.commons.lang.StringUtils.countMatches;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.core.query.Update.update;
import static org.springframework.data.mongodb.gridfs.GridFsCriteria.whereFilename;
import static org.springframework.data.mongodb.gridfs.GridFsCriteria.whereMetaData;


@ManagedResource
@Service
public class GridFsService {
  private static final Logger LOGGER = LoggerFactory.getLogger(GridFsService.class);
  private static final int BUFFER_SIZE = 16 * 1024 * 1024;
  public static final String HAS_DESCRIPTOR_READ_PERMISSION = "hasPermission(#descriptor, '" + READ_FILE + "')";

  private final FileStorageService fileStorageService;
  private final MongoTemplate mongoTemplate;
  private final YumEntriesRepository yumEntriesRepository;
  private final RepoService repoService;
  private YumPackageVersionComparator comparator = new YumPackageVersionComparator();

  //needed for cglib proxy
  public GridFsService() {
    this.mongoTemplate = null;
    this.yumEntriesRepository = null;
    this.fileStorageService = null;
    this.repoService = null;
  }

  @Autowired
  public GridFsService(FileStorageService fileStorageService, MongoTemplate mongoTemplate,
                       YumEntriesRepository yumEntriesRepository, RepoService repoService) {
    this.fileStorageService = fileStorageService;
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

    FileStorageItem storageItem = findRpmByPathDirectlyOrFindNewestRpmMatchingNameAndArch(descriptor);
    if (storageItem == null) {
      throw new GridFSFileNotFoundException("Could not find file.", sourceFile);
    }
    if (storageItem.isMarkedAsDeleted()) {
      throw new GridFSFileNotFoundException("Could not propagate file. File is marked for deletion.", sourceFile);
    }

    String sourceRepo = storageItem.getRepo();
    GridFsFileDescriptor fileDescriptor = move(storageItem, destinationRepo);
    repoService.createOrUpdate(sourceRepo);
    repoService.createOrUpdate(destinationRepo);
    return fileDescriptor;
  }


  @TimeMeasurement
  @PreAuthorize(HAS_DESCRIPTOR_READ_PERMISSION)
  public FileStorageItem findFileByDescriptor(GridFsFileDescriptor descriptor) {
    return internalUnsecuredFindFileByDescriptor(descriptor);
  }

  @TimeMeasurement
  public FileStorageItem internalUnsecuredFindFileByDescriptor(GridFsFileDescriptor descriptor) {
    return fileStorageService.findBy(descriptor);
  }


  @TimeMeasurement
  @PreAuthorize(HAS_DESCRIPTOR_READ_PERMISSION)
  public FileStorageItem getFileByDescriptor(GridFsFileDescriptor descriptor) {
    FileStorageItem storageItem = findFileByDescriptor(descriptor);
    if (storageItem == null) {
      throw new GridFSFileNotFoundException("Could not find file in gridfs.", descriptor.getPath());
    }
    return storageItem;
  }

  @TimeMeasurement
  public void delete(GridFsFileDescriptor descriptor) {
    FileStorageItem dbFile = getFileByDescriptor(descriptor);
    delete(dbFile);

    String sourceRepo = dbFile.getRepo();
    if (sourceRepo != null) {
      repoService.createOrUpdate(sourceRepo);
    }
  }

  @TimeMeasurement
  public void delete(List<FileStorageItem> storageItems) {
    for (FileStorageItem storageItem : storageItems) {
      delete(storageItem);
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

  private FileStorageItem findRpmByPathDirectlyOrFindNewestRpmMatchingNameAndArch(GridFsFileDescriptor descriptor) {
    FileStorageItem dbFile = findFileByDescriptor(descriptor);
    if (dbFile != null) {
      if (!dbFile.getFilename().endsWith(".rpm")) {
        throw new BadRequestException("Source rpm must end with .rpm!");
      }
      return dbFile;
    }

    return findNewestRpmByPath(descriptor);
  }

  private FileStorageItem findNewestRpmByPath(GridFsFileDescriptor descriptor) {
    return findNewestRpmInRepoByNameAndArch(descriptor.getRepo(), descriptor.getArch(), descriptor.getFilename());
  }

  private FileStorageItem findNewestRpmInRepoByNameAndArch(String repo, String arch, String name) {
    List<YumEntry> entries = yumEntriesRepository.findByRepoAndYumPackageArchAndYumPackageName(repo, arch, name);
    if (entries.isEmpty()) {
      return null;
    }

    sort(entries, new ArgumentComparator<>(on(YumEntry.class).getYumPackage().getVersion(), comparator));

    YumEntry lastEntry = entries.get(entries.size() - 1);
    return fileStorageService.findById(lastEntry.getId());
  }

  private GridFsFileDescriptor move(FileStorageItem storageItem, String destinationRepo) {
    YumEntry yumEntry = yumEntriesRepository.findOne((ObjectId) storageItem.getId());
    if (yumEntry == null) {
      try {
        yumEntry = regenerateMetadataFor(storageItem);
      } catch (InvalidRpmHeaderException e) {
        throw new RuntimeException("Could not regenerate metadata for file " + storageItem.getFilename() + " because it is not a valid RPM. You should manually delete the file", e);
      }
    }
    yumEntry.setRepo(null);
    yumEntriesRepository.save(yumEntry);

    GridFsFileDescriptor descriptor = new GridFsFileDescriptor(storageItem);
    descriptor.setRepo(destinationRepo);

    FileStorageItem storageItemToOverride = findFileByDescriptor(descriptor);
    fileStorageService.moveTo(storageItem, destinationRepo);

    if (storageItemToOverride != null) {
      delete(storageItemToOverride);
    }

    yumEntry.setRepo(destinationRepo);
    yumEntriesRepository.save(yumEntry);

    return descriptor;
  }

  @PreAuthorize(HAS_DESCRIPTOR_READ_PERMISSION)
  public BoundedGridFsResource getResource(GridFsFileDescriptor descriptor) throws IOException {
    return getResource(descriptor, 0);
  }

  @PreAuthorize(HAS_DESCRIPTOR_READ_PERMISSION)
  public BoundedGridFsResource getResource(GridFsFileDescriptor descriptor, long startPos) throws IOException {
    return new BoundedGridFsResource(getFileStorageItemWithCheckedStartPos(descriptor, startPos), startPos);
  }

  @PreAuthorize(HAS_DESCRIPTOR_READ_PERMISSION)
  public BoundedGridFsResource getResource(GridFsFileDescriptor descriptor, long startPos, long size)
                                    throws IOException {
    return new BoundedGridFsResource(getFileStorageItemWithCheckedStartPos(descriptor, startPos), startPos, size);
  }

  private FileStorageItem getFileStorageItemWithCheckedStartPos(GridFsFileDescriptor descriptor, long startPos) {
    FileStorageItem storageItem = getFileByDescriptor(descriptor);
    if (startPos >= storageItem.getSize()) {
      throw new BadRangeRequestException(format(
          "Range start is bigger than file size.\n" +
              "\tpath: %s\n" +
              "\tstartPos: %s\n" +
              "\tlength: %s\n", descriptor.getPath(), startPos, storageItem.getSize()));
    }
    return storageItem;
  }

  @PreAuthorize("hasPermission(#sourceRepo, '" + PROPAGATE_REPO + "')")
  public void propagateRepository(String sourceRepo, String destinationRepo) {
    validateRepoName(sourceRepo);
    validateDestinationRepo(sourceRepo, destinationRepo);

    List<FileStorageItem> sourceStorageItems = fileStorageService.getAllRpms(sourceRepo);
    for (FileStorageItem storageItem : sourceStorageItems) {
      if (!storageItem.isMarkedAsDeleted()) {
        move(storageItem, destinationRepo);
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
    final FileStorageItem storageItem = fileStorageService.storeFile(bufferedInputStream, descriptor);

    yumEntriesRepository.save(createYumEntry(yumPackage, storageItem));
    repoService.createOrUpdate(reponame);
    LOGGER.info("Stored RPM {}/{}", reponame, yumPackage.getLocation().getHref());
  }

  @TimeMeasurement
  public Data storeRepodataDbBz2(String reponame, File metadataFile, String name) throws IOException {
    validateRepoName(reponame);

    UploadResult uploadResult = fileStorageService.storeSqliteFileCompressedWithChecksumName(reponame, metadataFile, name);
    return createRepoMdData(uploadResult);
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
  public YumEntry regenerateMetadataFor(GridFsFileDescriptor descriptor) throws InvalidRpmHeaderException {
    return regenerateMetadataFor(getFileByDescriptor(descriptor));
  }

  @ManagedOperation
  public void regenerateMetadataForAllFiles() throws InvalidRpmHeaderException {
    for (FileStorageItem storageItem : fileStorageService.getAllRpms()) {
      regenerateMetadataFor(storageItem);
    }
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

  private void delete(FileStorageItem storageItem) {
    yumEntriesRepository.delete((ObjectId) storageItem.getId());
    fileStorageService.delete(storageItem);
  }

  private YumPackage convertHeader(InputStream inputStream) throws InvalidRpmHeaderException {
    RpmHeaderWrapper headerWrapper = new RpmHeaderWrapper(readHeader(inputStream));
    RpmHeaderToYumPackageConverter converter = new RpmHeaderToYumPackageConverter(headerWrapper);
    return converter.convert();
  }

  private YumEntry createYumEntry(YumPackage yumPackage, FileStorageItem storageItem) {
    yumPackage.getSize().setPackaged((int) storageItem.getSize());
    yumPackage.setChecksum(new YumPackageChecksum("sha256", storageItem.getChecksumSha256()));
    yumPackage.getTime().setFile(yumTime(storageItem.getUploadDate()));
    return new YumEntry((ObjectId) storageItem.getId(), storageItem.getRepo(), yumPackage);
  }

  private Data createRepoMdData(UploadResult uploadResult) {
    Data data = new Data();
    data.setChecksum(SHA256_KEY, uploadResult.getCompressedChecksum());
    data.setSize(uploadResult.getCompressedSize());
    data.setOpenSize(uploadResult.getUncompressedSize());
    data.setOpenChecksum(SHA256_KEY, uploadResult.getUncompressedChecksum());
    data.setLocation(substringAfter(uploadResult.getLocation(), "/"));
    data.setTimestamp((int) (uploadResult.getUploadDate().getTime() / 1000));
    data.setDatabaseVersion(DB_VERSION);
    return data;
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

  private YumEntry regenerateMetadataFor(FileStorageItem storageItem) throws InvalidRpmHeaderException {
    LOGGER.info("regenerating metadata for {}", storageItem.getFilename());
    try {
      YumPackage yumPackage = convertHeader(storageItem.getInputStream());
      YumEntry yumEntry = createYumEntry(yumPackage, storageItem);
      yumEntriesRepository.save(yumEntry);
      return yumEntry;
    } catch (InvalidRpmHeaderException e) {
      LOGGER.error("Generating metadata for " + storageItem.getFilename() + " failed.", e);
      throw e;
    }
  }
}
