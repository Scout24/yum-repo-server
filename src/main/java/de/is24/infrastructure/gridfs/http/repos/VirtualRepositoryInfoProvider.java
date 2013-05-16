package de.is24.infrastructure.gridfs.http.repos;

import com.mongodb.DBObject;
import de.is24.infrastructure.gridfs.http.domain.Container;
import de.is24.infrastructure.gridfs.http.domain.FileInfo;
import de.is24.infrastructure.gridfs.http.domain.FolderInfo;
import de.is24.infrastructure.gridfs.http.domain.RepoEntry;
import de.is24.infrastructure.gridfs.http.domain.RepoType;
import de.is24.infrastructure.gridfs.http.domain.SortField;
import de.is24.infrastructure.gridfs.http.domain.SortOrder;
import de.is24.infrastructure.gridfs.http.exception.RepositoryNotFoundException;
import de.is24.infrastructure.gridfs.http.metadata.RepoEntriesRepository;
import de.is24.infrastructure.gridfs.http.monitoring.TimeMeasurement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static de.is24.infrastructure.gridfs.http.domain.FolderInfo.fromRepoEntry;
import static de.is24.infrastructure.gridfs.http.domain.RepoType.VIRTUAL;
import static de.is24.infrastructure.gridfs.http.domain.SortField.name;
import static de.is24.infrastructure.gridfs.http.domain.SortOrder.asc;
import static java.lang.String.format;


@Service
public class VirtualRepositoryInfoProvider implements RepositoryInfoProvider {
  private final RepoEntriesRepository entriesRepository;
  private final RepositoryInfoProvider staticRepositoryInfoProvider;

  @Autowired
  public VirtualRepositoryInfoProvider(RepoEntriesRepository entriesRepository,
                                       RepositoryInfoProvider staticRepositoryInfoProvider) {
    this.entriesRepository = entriesRepository;
    this.staticRepositoryInfoProvider = staticRepositoryInfoProvider;
  }

  @Override
  @TimeMeasurement
  public Container<FolderInfo> getRepos(SortField sortBy, SortOrder sortOrder) {
    final Sort sort = new Sort(sortOrder.asDirection(), sortBy.name());

    List<RepoEntry> virtualRepos = entriesRepository.findByType(VIRTUAL, sort);
    return new Container<>("virtual", adaptVirtualRepos(virtualRepos));
  }

  @Override
  public Container<FolderInfo> getArchs(String repoName, SortField sortBy, SortOrder sortOrder) {
    Container<FolderInfo> staticArchsContainer = staticRepositoryInfoProvider.getArchs(
      getVirtualRepoOrThrowNotFoundException(repoName).getTarget(), sortBy, sortOrder);
    return new Container<>(pathWithSuffix(repoName), staticArchsContainer);
  }

  private RepoEntry getVirtualRepoOrThrowNotFoundException(String repoName) throws RepositoryNotFoundException {
    RepoEntry virtualRepo = entriesRepository.findFirstByNameAndType(repoName, VIRTUAL);
    if (virtualRepo == null) {
      throw new RepositoryNotFoundException("Virtual repository not found.", repoName);
    }
    return virtualRepo;
  }

  @Override
  public Container<FileInfo> getFileInfo(String repoName, String arch,
                                         SortField sortBy, SortOrder sortOrder) {
    RepoEntry virtualRepo = getVirtualRepoOrThrowNotFoundException(repoName);
    Container<FileInfo> staticFileInfo = staticRepositoryInfoProvider.getFileInfo(virtualRepo.getTarget(), arch,
      sortBy, sortOrder);
    return new Container<>(pathWithSuffix(format("%s/%s", repoName, arch)), staticFileInfo);
  }

  @Override
  public RepoType[] getValidRepoTypes() {
    return new RepoType[] { VIRTUAL };
  }

  private static String pathWithSuffix(String suffix) {
    return "virtual/" + suffix;
  }

  private Set<FolderInfo> adaptVirtualRepos(List<RepoEntry> virtualRepos) {
    Map<String, Long> sizeByName = getSizeByName();
    Set<FolderInfo> result = newLinkedHashSet();
    for (RepoEntry virtualRepo : virtualRepos) {
      Long size = sizeByName.get(virtualRepo.getTarget());
      if (size == null) {
        size = 0L;
      }

      FolderInfo folderInfo = fromRepoEntry(virtualRepo, size);
      result.add(folderInfo);
    }
    return result;
  }

  Map<String, Long> getSizeByName() {
    Iterable<DBObject> dbObjects = staticRepositoryInfoProvider.getReposAggregation(name, asc);
    Map<String, Long> result = newLinkedHashMap();
    for (DBObject object : dbObjects) {
      FolderInfo folderInfo = new FolderInfo(object);
      result.put(folderInfo.getName(), folderInfo.getSize());
    }
    return result;
  }

  @Override
  public Container<FileInfo> find(String filenameRegex, String repo, String arch, SortField sortBy,
                                  SortOrder sortOrder) {
    return null;
  }

  @Override
  public Container<FileInfo> find(String filenameRegex, String repo, SortField sortBy, SortOrder sortOrder) {
    return null;
  }

  @Override
  public Container<FileInfo> find(String filenameRegex, SortField sortBy, SortOrder sortOrder) {
    return null;
  }

  @Override
  public Iterable<DBObject> getReposAggregation(SortField sortBy, SortOrder sortOrder) {
    // "this is not defined for virtual repos"
    throw new NotImplementedException();
  }

  @Override
  public List<RepoEntry> find(String repoNameRegex, String tag, Date newer, Date older) {
    return null;
  }

  @Override
  public List<RepoEntry> find(String repoNameRegex, Date newer, Date older) {
    return null;
  }

  @Override
  public List<RepoEntry> find(String repoNameRegex) {
    return entriesRepository.findByTypeAndNameStartsWith(VIRTUAL, repoNameRegex);
  }
}
