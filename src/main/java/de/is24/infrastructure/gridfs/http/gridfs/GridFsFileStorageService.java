package de.is24.infrastructure.gridfs.http.gridfs;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSFile;
import com.mongodb.gridfs.GridFSInputFile;
import com.mongodb.gridfs.GridFSUtil;
import de.is24.infrastructure.gridfs.http.exception.BadRangeRequestException;
import de.is24.infrastructure.gridfs.http.exception.GridFSFileAlreadyExistsException;
import de.is24.infrastructure.gridfs.http.exception.GridFSFileNotFoundException;
import de.is24.infrastructure.gridfs.http.storage.FileDescriptor;
import de.is24.infrastructure.gridfs.http.storage.FileStorageItem;
import de.is24.infrastructure.gridfs.http.storage.FileStorageService;
import de.is24.infrastructure.gridfs.http.storage.UploadResult;
import de.is24.util.monitoring.spring.TimeMeasurement;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.tx.MongoTx;
import org.springframework.http.MediaType;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

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

import static com.mongodb.gridfs.GridFSUtil.mergeMetaData;
import static com.mongodb.gridfs.GridFSUtil.remove;
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
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.SHA256_KEY;
import static de.is24.infrastructure.gridfs.http.security.Permission.HAS_DESCRIPTOR_READ_PERMISSION;
import static java.lang.String.format;
import static java.util.regex.Pattern.quote;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.apache.commons.codec.digest.DigestUtils.getSha256Digest;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.core.query.Update.update;
import static org.springframework.data.mongodb.gridfs.GridFsCriteria.whereFilename;
import static org.springframework.data.mongodb.gridfs.GridFsCriteria.whereMetaData;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

@ManagedResource
@Service
public class GridFsFileStorageService implements FileStorageService {
  private static final Logger LOGGER = LoggerFactory.getLogger(GridFsFileStorageService.class);
  public static final String CONTENT_TYPE_APPLICATION_X_RPM = "application/x-rpm";
  public static final String CONTENT_TYPE_APPLICATION_X_GPG = "application/x-gpg";
  private static final String BZ2_CONTENT_TYPE = new MediaType("application", "x-bzip2").toString();
  private static final String ENDS_WITH_RPM_REGEX = ".*\\.rpm$";
  private static final int MB = 1024 * 1024;
  private static final int FIVE_MB = 5 * MB;

  private final GridFS gridFs;
  private final GridFsOperations gridFsTemplate;
  private final MongoTemplate mongoTemplate;

  @Autowired
  public GridFsFileStorageService(GridFS gridFs, GridFsOperations gridFsTemplate, MongoTemplate mongoTemplate) {
    this.gridFs = gridFs;
    this.gridFsTemplate = gridFsTemplate;
    this.mongoTemplate = mongoTemplate;

    setupIndices();
  }

  @Override
  public FileStorageItem findById(Object id) {
    if (!(id instanceof ObjectId)) {
      throw new IllegalArgumentException("id must be of type ObjectId, but got: " + id.getClass());
    }
    GridFSDBFile gridFSDBFile = gridFs.find((ObjectId) id);
    return gridFSDBFile != null ? new GridFsFileStorageItem(gridFSDBFile) : null;
  }

  @TimeMeasurement
  @PreAuthorize(HAS_DESCRIPTOR_READ_PERMISSION)
  @Override
  public FileStorageItem findBy(FileDescriptor descriptor) {
    return insecureFindBy(descriptor);
  }

  @TimeMeasurement
  private FileStorageItem insecureFindBy(FileDescriptor descriptor) {
    GridFSDBFile gridFSDBFile = gridFsTemplate.findOne(query(whereFilename().is(descriptor.getPath())));
    return gridFSDBFile != null ? new GridFsFileStorageItem(gridFSDBFile) : null;
  }

  @TimeMeasurement
  @PreAuthorize(HAS_DESCRIPTOR_READ_PERMISSION)
  @Override
  public FileStorageItem getFileBy(FileDescriptor descriptor) {
    FileStorageItem storageItem = findBy(descriptor);
    if (storageItem == null) {
      throw new GridFSFileNotFoundException("Could not find file in gridfs.", descriptor.getPath());
    }
    return storageItem;
  }

  @Override
  public void delete(FileStorageItem storageItem) {
    remove(((GridFsFileStorageItem) storageItem).getDbFile());
  }

  @Override
  public void delete(FileDescriptor descriptor) {
    FileStorageItem storageItem = findBy(descriptor);
    if (storageItem != null) {
      delete(storageItem);
    }
  }

  @Override
  public void moveTo(FileStorageItem storageItem, String repo) {
    GridFSDBFile dbFile = ((GridFsFileStorageItem) storageItem).getDbFile();
    FileDescriptor descriptor = new FileDescriptor(storageItem);
    descriptor.setRepo(repo);
    dbFile.put(FILENAME_KEY, descriptor.getPath());
    dbFile.getMetaData().put(REPO_KEY, repo);
    dbFile.save();
  }

  @Override
  public List<FileStorageItem> getAllRpms() {
    return convert(gridFs.find(query(
        whereFilename().regex(ENDS_WITH_RPM_REGEX)
        .and(METADATA_MARKED_AS_DELETED_KEY).is(null)).getQueryObject()));
  }

  @Override
  public List<FileStorageItem> getAllRpms(String repo) {
    return convert(gridFsTemplate.find(query(
        whereMetaData(REPO_KEY).is(repo)
            .and(METADATA_MARKED_AS_DELETED_KEY)
            .is(null)
            .andOperator(whereFilename().regex(ENDS_WITH_RPM_REGEX)))));
  }

  @Override
  @MongoTx
  public FileStorageItem storeFile(InputStream inputStream, FileDescriptor descriptor) {
    return storeFile(inputStream, descriptor, false);
  }

  @Override
  @MongoTx
  public FileStorageItem storeFile(InputStream inputStream, FileDescriptor descriptor, boolean allowOverride) {
    FileStorageItem existingDbFile = findBy(descriptor);
    if (existingDbFile != null && !allowOverride) {
      throw new GridFSFileAlreadyExistsException("Reupload of rpm is not possible.", descriptor.getPath());
    }

    DigestInputStream digestInputStream = new DigestInputStream(inputStream, getSha256Digest());
    GridFSFile inputFile = gridFsTemplate.store(digestInputStream, descriptor.getPath(), getContentType(descriptor.getPath()));
    closeQuietly(digestInputStream);

    String sha256Hash = encodeHexString(digestInputStream.getMessageDigest().digest());
    DBObject metaData = createBasicMetaDataObject(descriptor, sha256Hash);
    mergeMetaData(inputFile, metaData);

    inputFile.save();

    if (existingDbFile != null) {
      delete(existingDbFile);
    }

    return findById(inputFile.getId());
  }

  private String getContentType(String path) {
    if (path.endsWith(".rpm")) {
      return CONTENT_TYPE_APPLICATION_X_RPM;
    } else if (path.endsWith(".xml")) {
      return APPLICATION_XML_VALUE;
    } else if (path.endsWith(".asc")) {
      return CONTENT_TYPE_APPLICATION_X_GPG;
    }

    return APPLICATION_OCTET_STREAM_VALUE;
  }

  @Override
  public UploadResult storeSqliteFileCompressedWithChecksumName(String reponame, File metadataFile, String name) throws IOException {
    FileDescriptor descriptor = new FileDescriptor(reponame, ARCH_KEY_REPO_DATA, name);
    GridFSInputFile inputFile = gridFs.createFile();
    inputFile.setContentType(BZ2_CONTENT_TYPE);

    InputStream fileInputStream = new BufferedInputStream(new FileInputStream(metadataFile));
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

    String compressedChecksum = encodeHexString(compressedDigestOutputStream.getMessageDigest().digest());
    String uncompressedChecksum = encodeHexString(uncompressedDigestInputStream.getMessageDigest().digest());
    String finalFilename = reponame + "/" + createRepoMdLocation(name, compressedChecksum);

    gridFs.remove(finalFilename);

    DBObject metaData = createBasicMetaDataObject(descriptor, compressedChecksum);

    inputFile.setMetaData(metaData);
    inputFile.put(FILENAME_KEY, finalFilename);
    inputFile.getOutputStream().close();

    UploadResult uploadResult = new UploadResult();
    uploadResult.setLocation(finalFilename);
    uploadResult.setUploadDate(inputFile.getUploadDate());
    uploadResult.setCompressedSize(inputFile.getLength());
    uploadResult.setCompressedChecksum(compressedChecksum);
    uploadResult.setUncompressedSize(metadataFile.length());
    uploadResult.setUncompressedChecksum(uncompressedChecksum);
    return uploadResult;
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

  public void markForDeletionByPath(final String path) {
    markForDeletion(whereFilename().is(path));
  }

  public void markForDeletionByFilenameRegex(final String regex) {
    markForDeletion(whereFilename().regex(regex));
  }

  @Override
  public void deleteRepo(String reponame) {
    markForDeletion(whereMetaData(REPO_KEY).is(reponame));
  }

  @Override
  public List<FileStorageItem> findByPrefix(String prefix) {
    return convert(gridFsTemplate.find(query(whereFilename().regex("^" + quote(prefix)))));
  }

  @Override
  public void setUploadDate(FileStorageItem file, Date date) {
    Assert.notNull(file);
    Assert.isInstanceOf(GridFsFileStorageItem.class, file);
    GridFSDBFile dbFile = ((GridFsFileStorageItem) file).getDbFile();
    dbFile.getMetaData().put("uploadDate", date);
    dbFile.save();
  }

  @Override
  @PreAuthorize(HAS_DESCRIPTOR_READ_PERMISSION)
  public BoundedGridFsResource getResource(FileDescriptor descriptor) throws IOException {
    return getResource(descriptor, 0);
  }

  @Override
  @PreAuthorize(HAS_DESCRIPTOR_READ_PERMISSION)
  public BoundedGridFsResource getResource(FileDescriptor descriptor, long startPos, long size)
      throws IOException {
    return new BoundedGridFsResource(getFileStorageItemWithCheckedStartPos(descriptor, startPos), startPos, size);
  }

  @Override
  @MongoTx
  public List<FileStorageItem> getCorruptFiles() {
    return convert(gridFsTemplate.find(getCorruptFileQuery()));
  }

  @Override
  public void deleteCorruptFiles() {
    gridFsTemplate.delete(getCorruptFileQuery());
  }

  @Override
  @PreAuthorize(HAS_DESCRIPTOR_READ_PERMISSION)
  public BoundedGridFsResource getResource(FileDescriptor descriptor, long startPos) throws IOException {
    return new BoundedGridFsResource(getFileStorageItemWithCheckedStartPos(descriptor, startPos), startPos);
  }

  private List<FileStorageItem> convert(List<GridFSDBFile> rpms) {
    return rpms.stream().map(GridFsFileStorageItem::new).collect(toList());
  }

  private FileStorageItem getFileStorageItemWithCheckedStartPos(FileDescriptor descriptor, long startPos) {
    FileStorageItem storageItem = getFileBy(descriptor);
    if (startPos >= storageItem.getSize()) {
      throw new BadRangeRequestException(format(
          "Range start is bigger than file size.\n" +
              "\tpath: %s\n" +
              "\tstartPos: %s\n" +
              "\tlength: %s\n", descriptor.getPath(), startPos, storageItem.getSize()));
    }
    return storageItem;
  }

  private Query getCorruptFileQuery() {
    return query(new Criteria().orOperator(whereFilename().is(null), whereMetaData().is(null)));
  }

  private void markForDeletion(final Criteria criteria) {
    mongoTemplate.updateMulti(query(criteria.and(METADATA_MARKED_AS_DELETED_KEY).is(null)),
        update(METADATA_MARKED_AS_DELETED_KEY, new Date()),
        GRIDFS_FILES_COLLECTION);
  }

  private String createRepoMdLocation(String name, String checksum) {
    return ARCH_KEY_REPO_DATA + "/" + name + "-" + checksum + ".sqlite.bz2";
  }

  private DBObject createBasicMetaDataObject(FileDescriptor descriptor, String sha256Hash) {
    DBObject metaData = new BasicDBObject();
    metaData.put(REPO_KEY, descriptor.getRepo());
    metaData.put(ARCH_KEY, descriptor.getArch());
    metaData.put(SHA256_KEY, sha256Hash);
    return metaData;
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

  private void setupIndices() {
    DBCollection filesCollection = mongoTemplate.getCollection(GRIDFS_FILES_COLLECTION);
    filesCollection.ensureIndex(METADATA_REPO_KEY);
    filesCollection.ensureIndex(METADATA_ARCH_KEY);
    filesCollection.ensureIndex(METADATA_UPLOAD_DATE_KEY);
    filesCollection.ensureIndex(METADATA_MARKED_AS_DELETED_KEY);
  }
}
