package de.is24.infrastructure.gridfs.http.repos;

import com.mongodb.DBObject;
import de.is24.infrastructure.gridfs.http.domain.Container;
import de.is24.infrastructure.gridfs.http.domain.FileInfo;
import de.is24.infrastructure.gridfs.http.domain.FolderInfo;
import de.is24.infrastructure.gridfs.http.domain.RepoEntry;
import de.is24.infrastructure.gridfs.http.domain.RepoType;
import de.is24.infrastructure.gridfs.http.domain.SortField;
import de.is24.infrastructure.gridfs.http.domain.SortOrder;
import java.util.Date;
import java.util.List;


public interface RepositoryInfoProvider {
  Container<FolderInfo> getRepos(SortField sortBy, SortOrder sortOrder);

  Container<FolderInfo> getArchs(String reponame, SortField sortBy, SortOrder sortOrder);

  Container<FileInfo> getFileInfo(String repoName, String arch, SortField sortBy, SortOrder sortOrder);

  Container<FileInfo> find(String filenameRegex, String repo, String arch, SortField sortBy, SortOrder sortOrder);

  Container<FileInfo> find(String filenameRegex, String repo, SortField sortBy, SortOrder sortOrder);

  Container<FileInfo> find(String filenameRegex, SortField sortBy, SortOrder sortOrder);

  RepoType[] getValidRepoTypes();

  Iterable<DBObject> getReposAggregation(SortField sortBy, SortOrder sortOrder);

  List<RepoEntry> find(String repoNameRegex, String tag, Date newer, Date older);

  List<RepoEntry> find(String repoNameRegex, Date newer, Date older);

  List<RepoEntry> find(String repoNameRegex);
}
