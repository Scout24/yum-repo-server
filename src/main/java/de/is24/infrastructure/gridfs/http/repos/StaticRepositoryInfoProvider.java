package de.is24.infrastructure.gridfs.http.repos;

import ch.lambdaj.function.compare.ArgumentComparator;
import com.mongodb.AggregationOutput;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
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
import de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure;
import de.is24.infrastructure.gridfs.http.monitoring.TimeMeasurement;
import org.apache.commons.collections.ComparatorUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import static ch.lambdaj.Lambda.index;
import static ch.lambdaj.Lambda.on;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static de.is24.infrastructure.gridfs.http.domain.RepoType.SCHEDULED;
import static de.is24.infrastructure.gridfs.http.domain.RepoType.STATIC;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.FILENAME_KEY;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.GRIDFS_FILES_COLLECTION;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.METADATA_ARCH_KEY;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.METADATA_REPO_KEY;
import static de.is24.infrastructure.gridfs.http.mongo.MongoAggregationBuilder.groupBy;
import static de.is24.infrastructure.gridfs.http.mongo.MongoAggregationBuilder.match;
import static de.is24.infrastructure.gridfs.http.mongo.MongoAggregationBuilder.sort;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.gridfs.GridFsCriteria.whereMetaData;


@Service
public class StaticRepositoryInfoProvider implements RepositoryInfoProvider {
  private static final Pattern PATTERN = Pattern.compile("/+$");
  private final MongoTemplate mongoTemplate;
  private final RepoEntriesRepository entriesRepository;
  private static final Set<RepoType> STATIC_TYPES = new HashSet<>(Arrays.asList(RepoType.STATIC, RepoType.SCHEDULED));

  @Autowired
  public StaticRepositoryInfoProvider(MongoTemplate mongoTemplate, RepoEntriesRepository entriesRepository) {
    this.mongoTemplate = mongoTemplate;
    this.entriesRepository = entriesRepository;
  }

  @Override
  @TimeMeasurement
  public Container<FolderInfo> getRepos(SortField sortBy, SortOrder sortOrder) {
    final Iterable<DBObject> reposAggregation = getReposAggregation(sortBy, sortOrder);
    Set<FolderInfo> folderInfos = adaptFolders(reposAggregation);

    return new Container<>("", addEmptyRepositories(folderInfos, sortBy, sortOrder));
  }

  private Set<FolderInfo> addEmptyRepositories(Set<FolderInfo> folderInfos, SortField sortBy, SortOrder sortOrder) {
    final List<RepoEntry> staticRepos = entriesRepository.findByTypeIn(RepoType.SCHEDULED, RepoType.STATIC);
    for (RepoEntry staticRepo : staticRepos) {
      folderInfos.add(FolderInfo.fromRepoEntry(staticRepo, 0));
    }

    return sortFolderInfos(folderInfos, sortBy, sortOrder);
  }

  private Set<FolderInfo> sortFolderInfos(Set<FolderInfo> folderInfos, SortField sortBy, SortOrder sortOrder) {
    List<FolderInfo> list = new ArrayList<>(folderInfos);
    Comparator<FolderInfo> comparator = getComparator(sortBy);
    if (sortOrder == SortOrder.asc) {
      Collections.sort(list, comparator);
    } else {
      Collections.sort(list, getReversedComparator(comparator));
    }
    return new LinkedHashSet<>(list);
  }

  @SuppressWarnings("unchecked")
  private Comparator<FolderInfo> getReversedComparator(final Comparator<FolderInfo> comparator) {
    return ComparatorUtils.reversedComparator(comparator);
  }

  private Comparator<FolderInfo> getComparator(SortField sortBy) {
    switch (sortBy) {
      case uploadDate:
      case size: {
        return new ArgumentComparator<>(on(FolderInfo.class).getLastModified());
      }

      default: {
        return new ArgumentComparator<>(on(FolderInfo.class).getName());
      }
    }
  }

  @Override
  public Iterable<DBObject> getReposAggregation(SortField sortBy, SortOrder sortOrder) {
    AggregationOutput aggregation = getFilesCollection().aggregate(
      groupBy(DatabaseStructure.METADATA_REPO_KEY) //
      .sum("length").max("uploadDate").build(),
      sort(sortBy, sortOrder));
    return aggregation.results();
  }

  @Override
  @TimeMeasurement
  public Container<FolderInfo> getArchs(String reponame, SortField sortBy, SortOrder sortOrder) {
    verifyRepositoryExists(reponame);

    AggregationOutput aggregation = getFilesCollection().aggregate(match(
      whereMetaData(DatabaseStructure.REPO_KEY) //
      .is(reponame)),
      groupBy(DatabaseStructure.METADATA_ARCH_KEY).sum("length").max("uploadDate").build(),
      sort(sortBy, sortOrder));
    return new Container<>(reponame, adaptFolders(aggregation.results()));
  }

  private void verifyRepositoryExists(final String reponame) {
    if (null == entriesRepository.findFirstByName(reponame)) {
      throw new RepositoryNotFoundException("Repository not found.", reponame);
    }
  }

  @Override
  @TimeMeasurement
  public Container<FileInfo> getFileInfo(String reponame, String arch, SortField sortBy, SortOrder sortOrder) {
    verifyRepositoryExists(reponame);

    DBCursor cursor = getFilesCollection().find(query(
      whereMetaData(DatabaseStructure.REPO_KEY) //
      .is(reponame).andOperator(whereMetaData(DatabaseStructure.ARCH_KEY).is(arch))).getQueryObject())
      .sort(sortBy.sortFile(sortOrder));
    return new Container<>(reponame + "/" + arch, adaptFiles(cursor.iterator()));
  }

  @Override
  @TimeMeasurement
  public Container<FileInfo> find(String filenameRegex, SortField sortBy,
                                  SortOrder sortOrder) {
    return find(filenameRegex, "", "", sortBy, sortOrder);
  }

  @Override
  @TimeMeasurement
  public Container<FileInfo> find(String filenameRegex, String repo, SortField sortBy, SortOrder sortOrder) {
    return find(filenameRegex, repo, "", sortBy, sortOrder);
  }

  @Override
  @TimeMeasurement
  public Container<FileInfo> find(String filenameRegex, String repo, String arch, SortField sortBy,
                                  SortOrder sortOrder) {
    DBCursor cursor = getFilesCollection().find(
      query(
        where(FILENAME_KEY) //
        .regex(filenameRegex).and(METADATA_REPO_KEY).regex(addLineEndings(repo)) //
        .and(METADATA_ARCH_KEY).regex(addLineEndings(arch))) //
      .getQueryObject())
      .sort(sortBy.sortFile(sortOrder));

    return new Container<>(removeTrailingSlashes(repo + "/" + arch), adaptFiles(cursor));
  }

  @Override
  public RepoType[] getValidRepoTypes() {
    return new RepoType[] { STATIC, SCHEDULED };
  }

  private String removeTrailingSlashes(String s) {
    return PATTERN.matcher(s).replaceAll("");
  }

  private DBCollection getFilesCollection() {
    return mongoTemplate.getCollection(GRIDFS_FILES_COLLECTION);
  }

  private String addLineEndings(String s) {
    return isNullOrEmpty(s) ? ".*" : ('^' + s + '$');
  }

  private Set<FolderInfo> adaptFolders(Iterable<DBObject> dbObjects) {
    Set<FolderInfo> result = newLinkedHashSet();

    Map<String, RepoEntry> repoEntries = getRepoEntriesByRepoName();

    for (DBObject object : dbObjects) {
      FolderInfo folderInfo = new FolderInfo(object);
      RepoEntry repoEntry = repoEntries.get(folderInfo.getName());
      if (repoEntry != null) {
        folderInfo.setTags(repoEntry.getTags());
      } else {
        folderInfo.setTags(Collections.<String>emptySet());
      }
      result.add(folderInfo);
    }
    return result;
  }

  private Map<String, RepoEntry> getRepoEntriesByRepoName() {
    final List<RepoEntry> repoEntries = entriesRepository.findByTypeIn(RepoType.STATIC, RepoType.SCHEDULED);

    return index(repoEntries, on(RepoEntry.class).getName());
  }

  @Override
  @TimeMeasurement
  public List<RepoEntry> find(String repoNameRegex, String tag, Date newer, Date older) {
    return entriesRepository //
      .findByTypeInAndNameStartsWithAndTagsContainsAndLastModifiedIsBetween(STATIC_TYPES, repoNameRegex, tag, newer,
        older);
  }

  @Override
  @TimeMeasurement
  public List<RepoEntry> find(String repoNameRegex, Date newer, Date older) {
    return entriesRepository //
      .findByTypeInAndNameStartsWithAndLastModifiedIsBetween(STATIC_TYPES, repoNameRegex, newer, older);
  }

  private Set<FileInfo> adaptFiles(Iterator<DBObject> itr) {
    Set<FileInfo> fileInfos = newLinkedHashSet();
    while (itr.hasNext()) {
      fileInfos.add(new FileInfo(itr.next()));
    }
    return fileInfos;
  }

  @Override
  public List<RepoEntry> find(String repoNameRegex) {
    return null;
  }
}
