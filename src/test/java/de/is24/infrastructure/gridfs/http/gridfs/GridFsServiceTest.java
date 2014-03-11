package de.is24.infrastructure.gridfs.http.gridfs;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import de.is24.infrastructure.gridfs.http.domain.RepoEntry;
import de.is24.infrastructure.gridfs.http.domain.YumEntry;
import de.is24.infrastructure.gridfs.http.exception.BadRequestException;
import de.is24.infrastructure.gridfs.http.exception.GridFSFileNotFoundException;
import de.is24.infrastructure.gridfs.http.exception.RepositoryIsUndeletableException;
import de.is24.infrastructure.gridfs.http.metadata.YumEntriesRepository;
import de.is24.infrastructure.gridfs.http.repos.RepoService;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import java.util.Arrays;
import java.util.Date;

import static de.is24.infrastructure.gridfs.http.domain.RepoType.SCHEDULED;
import static de.is24.infrastructure.gridfs.http.domain.RepoType.STATIC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class GridFsServiceTest {
  private GridFsService service;
  private GridFS gridFs;
  private GridFsTemplate gridFsTemplate;
  private MongoTemplate mongoTemplate;
  private YumEntriesRepository yumEntriesRepository;
  private DBCollection filesCollection;
  private RepoService repoService;

  @Before
  public void setUp() {
    gridFs = mock(GridFS.class);
    gridFsTemplate = mock(GridFsTemplate.class);
    filesCollection = mock(DBCollection.class);
    mongoTemplate = mock(MongoTemplate.class);
    when(mongoTemplate.getCollection(eq("fs.files"))).thenReturn(filesCollection);
    yumEntriesRepository = mock(YumEntriesRepository.class);
    when(yumEntriesRepository.findOne(any(ObjectId.class))).thenReturn(new YumEntry(null, null, null));
    repoService = mock(RepoService.class);

    service = new GridFsService(gridFs, gridFsTemplate, mongoTemplate, yumEntriesRepository, repoService);
  }

  @Test(expected = BadRequestException.class)
  public void throwExceptionForSourceNull() throws Exception {
    service.propagateRpm(null, "dest-repo");
  }

  @Test(expected = BadRequestException.class)
  public void throwExceptionForDestRepoNull() throws Exception {
    service.propagateRpm("repo/arch/file.rpm", null);
  }

  @Test(expected = BadRequestException.class)
  public void throwExceptionForInvalidSourcePathDepth() throws Exception {
    service.propagateRpm("to-low-path/file.rpm", "dest-repo");
  }

  @Test(expected = GridFSFileNotFoundException.class)
  public void throwExceptionForNotRpmSourceFile() throws Exception {
    service.propagateRpm("repo/arch/file.txt", "dest-repo");
  }

  @Test(expected = BadRequestException.class)
  public void throwExceptionForInvalidDestRepo() throws Exception {
    service.propagateRpm("repo/arch/file.rpm", "dest-repo/with-more-depth");
  }

  @Test(expected = GridFSFileNotFoundException.class)
  public void throwExceptionForFileNotFound() throws Exception {
    service.propagateRpm("repo/arch/file.rpm", "dest-repo");
  }

  @Test
  public void waitAfterDeletionOfLargeFile() throws Exception {
    GridFSDBFile file = mock(GridFSDBFile.class);
    when(file.getFilename()).thenReturn("theFilename");
    when(file.getLength()).thenReturn(6 * 1024 * 1024L);
    when(gridFsTemplate.find((Query) anyObject())).thenReturn(Arrays.asList(file));

    final long start = System.currentTimeMillis();

    service.removeFilesMarkedAsDeletedBefore(new Date());

    final long duration = System.currentTimeMillis() - start;
    assertThat(duration, is(greaterThanOrEqualTo(400L)));
  }

  @Test
  public void moveFileToNewRepo() throws Exception {
    DBObject dbObject = mock(DBObject.class);
    GridFSDBFile dbFile = mock(GridFSDBFile.class);
    when(dbFile.getFilename()).thenReturn("repo/arch/file.rpm");
    when(dbFile.getMetaData()).thenReturn(dbObject);
    when(gridFsTemplate.findOne(any(Query.class))).thenReturn(dbFile);

    service.propagateRpm("repo/arch/file.rpm", "dest-repo");

    verify(dbFile).put(eq("filename"), eq("dest-repo/arch/file.rpm"));
    verify(dbObject).put(eq("repo"), eq("dest-repo"));
    verify(dbFile).save();
  }

  @Test
  public void createIndices() throws Exception {
    verify(filesCollection).ensureIndex("metadata.repo");
    verify(filesCollection).ensureIndex("metadata.arch");
  }

  @Test(expected = RepositoryIsUndeletableException.class)
  public void failOnDeleteForUndeletableRepository() throws Exception {
    RepoEntry repoEntry = new RepoEntry();
    repoEntry.setUndeletable(true);
    when(repoService.ensureEntry("repo", STATIC, SCHEDULED)).thenReturn(repoEntry);
    service.deleteRepository("repo");
  }

  @Test(expected = BadRequestException.class)
  public void failOnPropagationOfRpmToSameRepo() throws Exception {
    service.propagateRpm("repo/arch/file.rpm", "repo");
  }

  @Test(expected = BadRequestException.class)
  public void failOnPropagationOfRepositoryToSameRepo() throws Exception {
    service.propagateRepository("repo", "repo");
  }
}
