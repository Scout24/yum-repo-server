package de.is24.infrastructure.gridfs.http.gridfs;

import ch.lambdaj.function.compare.ArgumentComparator;
import de.is24.infrastructure.gridfs.http.domain.YumEntry;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackage;
import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageChecksum;
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
import de.is24.infrastructure.gridfs.http.storage.FileDescriptor;
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
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.SHA256_KEY;
import static de.is24.infrastructure.gridfs.http.repos.RepositoryNameValidator.validateRepoName;
import static de.is24.infrastructure.gridfs.http.security.Permission.PROPAGATE_FILE;
import static de.is24.infrastructure.gridfs.http.security.Permission.PROPAGATE_REPO;
import static java.nio.channels.Channels.newChannel;
import static java.util.Collections.sort;
import static org.apache.commons.lang.StringUtils.countMatches;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.substringAfter;


@ManagedResource
@Service
public class GridFsService {
  private static final Logger LOGGER = LoggerFactory.getLogger(GridFsService.class);
  private static final int BUFFER_SIZE = 16 * 1024 * 1024;

  private final FileStorageService fileStorageService;
  private final YumEntriesRepository yumEntriesRepository;
  private final RepoService repoService;
  private YumPackageVersionComparator comparator = new YumPackageVersionComparator();

  //needed for cglib proxy
  public GridFsService() {
    this.yumEntriesRepository = null;
    this.fileStorageService = null;
    this.repoService = null;
  }

  @Autowired
  public GridFsService(FileStorageService fileStorageService,
                       YumEntriesRepository yumEntriesRepository, RepoService repoService) {
    this.fileStorageService = fileStorageService;
    this.yumEntriesRepository = yumEntriesRepository;
    this.repoService = repoService;
  }

  @TimeMeasurement
  @PreAuthorize("hasPermission(#sourceFile, '" + PROPAGATE_FILE + "')")
  public FileDescriptor propagateRpm(String sourceFile, String destinationRepo) {
    validatePathToRpm(sourceFile);

    FileDescriptor descriptor = new FileDescriptor(sourceFile);
    validateDestinationRepo(descriptor.getRepo(), destinationRepo);

    FileStorageItem storageItem = findRpmByPathDirectlyOrFindNewestRpmMatchingNameAndArch(descriptor);
    if (storageItem == null) {
      throw new GridFSFileNotFoundException("Could not find file.", sourceFile);
    }
    if (storageItem.isMarkedAsDeleted()) {
      throw new GridFSFileNotFoundException("Could not propagate file. File is marked for deletion.", sourceFile);
    }

    String sourceRepo = storageItem.getRepo();
    FileDescriptor fileDescriptor = move(storageItem, destinationRepo);
    repoService.createOrUpdate(sourceRepo);
    repoService.createOrUpdate(destinationRepo);
    return fileDescriptor;
  }


  @TimeMeasurement
  public void delete(FileDescriptor descriptor) {
    FileStorageItem dbFile = fileStorageService.getFileBy(descriptor);
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

  private FileStorageItem findRpmByPathDirectlyOrFindNewestRpmMatchingNameAndArch(FileDescriptor descriptor) {
    FileStorageItem dbFile = fileStorageService.findBy(descriptor);
    if (dbFile != null) {
      if (!dbFile.getFilename().endsWith(".rpm")) {
        throw new BadRequestException("Source rpm must end with .rpm!");
      }
      return dbFile;
    }

    return findNewestRpmByPath(descriptor);
  }

  private FileStorageItem findNewestRpmByPath(FileDescriptor descriptor) {
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

  private FileDescriptor move(FileStorageItem storageItem, String destinationRepo) {
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

    FileDescriptor descriptor = new FileDescriptor(storageItem);
    descriptor.setRepo(destinationRepo);

    FileStorageItem storageItemToOverride = fileStorageService.findBy(descriptor);
    fileStorageService.moveTo(storageItem, destinationRepo);

    if (storageItemToOverride != null) {
      delete(storageItemToOverride);
    }

    yumEntry.setRepo(destinationRepo);
    yumEntriesRepository.save(yumEntry);

    return descriptor;
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

    FileDescriptor descriptor = new FileDescriptor(reponame, yumPackage);
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

    yumEntriesRepository.deleteByRepo(reponame);
    fileStorageService.deleteRepo(reponame);
    repoService.delete(reponame);
  }

  @ManagedOperation
  public YumEntry regenerateMetadataFor(FileDescriptor descriptor) throws InvalidRpmHeaderException {
    return regenerateMetadataFor(fileStorageService.getFileBy(descriptor));
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
