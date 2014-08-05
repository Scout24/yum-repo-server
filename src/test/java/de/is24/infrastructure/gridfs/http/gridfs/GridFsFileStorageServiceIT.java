package de.is24.infrastructure.gridfs.http.gridfs;

import com.mongodb.BasicDBObject;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSFile;
import de.is24.infrastructure.gridfs.http.category.LocalExecutionOnly;
import de.is24.infrastructure.gridfs.http.storage.AbstractStorageServiceIT;
import de.is24.infrastructure.gridfs.http.storage.FileDescriptor;
import de.is24.infrastructure.gridfs.http.storage.FileStorageItem;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import static com.mongodb.gridfs.GridFSUtil.mergeMetaData;
import static de.is24.infrastructure.gridfs.http.gridfs.GridFsServiceIT.TESTING_ARCH;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.MARKED_AS_DELETED_KEY;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.REPO_KEY;
import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.uniqueRepoName;
import static org.apache.commons.lang.time.DateUtils.addDays;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.gridfs.GridFsCriteria.whereMetaData;

@Category(LocalExecutionOnly.class)
public class GridFsFileStorageServiceIT extends AbstractStorageServiceIT {

  @Test
  public void deleteFilesMarkedAsDeleted() throws Exception {
    final Date now = new Date();
    final String nothingToDeleteRepo = givenFullRepository();
    givenTowOfThreeFilesToBeDeleted(now);

    context.fileStorageService().removeFilesMarkedAsDeletedBefore(now);

    final List<GridFSDBFile> fileList = context.gridFsTemplate()
        .find(query(whereMetaData(MARKED_AS_DELETED_KEY).ne(null)));
    assertThat(fileList.size(), is(1));

    final List<GridFSDBFile> filesInNothingToDeleteRepo = context.gridFsTemplate()
        .find(query(whereMetaData(REPO_KEY).is(nothingToDeleteRepo)));
    assertThat(filesInNothingToDeleteRepo.size(), is(4));
  }

  @Test
  public void metaDataForDeletionIsSetOnlyOnce() throws Exception {
    final String repoName = givenFullRepository();
    final Date yesterday = DateUtils.addDays(new Date(), -1);
    final String file = "a_file_to_be_deleted";
    FileDescriptor descriptor = new FileDescriptor(repoName, TESTING_ARCH, file);
    givenFileToBeDeleted(descriptor, yesterday);

    context.fileStorageService().markForDeletionByPath(descriptor.getPath());

    final FileStorageItem storageItem = context.fileStorageService().findBy(descriptor);

    assertThat(storageItem.getDateOfMarkAsDeleted(), is(equalTo(yesterday)));
  }

  private void givenTowOfThreeFilesToBeDeleted(final Date now) throws IOException {
    final String repoToDeleteIn = uniqueRepoName();
    final Date past = addDays(now, -1);
    givenFileToBeDeleted(new FileDescriptor(repoToDeleteIn, TESTING_ARCH, "toBeDeletedPast1"), past);
    givenFileToBeDeleted(new FileDescriptor(repoToDeleteIn, TESTING_ARCH, "toBeDeletedPast2"), past);
    givenFileToBeDeleted(new FileDescriptor(repoToDeleteIn, TESTING_ARCH, "toBeDeletedFuture"), addDays(now, 1));
  }

  private GridFSFile givenFileToBeDeleted(FileDescriptor descriptor, final Date time) throws IOException {
    final GridFSDBFile toBeDeleted = ((GridFsFileStorageItem) givenFileWithDescriptor(descriptor)).getDbFile();

    mergeMetaData(toBeDeleted, new BasicDBObject(MARKED_AS_DELETED_KEY, time));
    toBeDeleted.save();
    return toBeDeleted;
  }
}
