package de.is24.infrastructure.gridfs.http.gridfs;

import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import java.util.Arrays;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GridFsFileStorageServiceTest {

  private GridFS gridFs;
  private GridFsTemplate gridFsTemplate;
  private GridFsFileStorageService service;
  private MongoTemplate mongoTemplate;

  @Before
  public void setUp() throws Exception {
    gridFs = mock(GridFS.class);
    gridFsTemplate = mock(GridFsTemplate.class);
    mongoTemplate = mock(MongoTemplate.class);
    service = new GridFsFileStorageService(gridFs, gridFsTemplate, mongoTemplate);
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
  public void shouldSetRepo() throws Exception {
    DBObject dbObject = mock(DBObject.class);
    GridFSDBFile dbFile = mock(GridFSDBFile.class);
    when(dbFile.getFilename()).thenReturn("src-repo/arch/file.rpm");
    when(dbFile.getMetaData()).thenReturn(dbObject);
    service.moveTo(new GridFsFileStorageItem(dbFile), "dest-repo");
    verify(dbFile).put(eq("filename"), eq("dest-repo/arch/file.rpm"));
    verify(dbObject).put(eq("repo"), eq("dest-repo"));
    verify(dbFile).save();
  }

  //TODO:

  @Test
  public void setCorrectContentType() throws Exception {

  }

  @Test
  public void overrideExistingFile() throws Exception {

  }
}