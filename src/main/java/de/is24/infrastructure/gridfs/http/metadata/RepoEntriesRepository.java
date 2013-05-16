package de.is24.infrastructure.gridfs.http.metadata;

import de.is24.infrastructure.gridfs.http.domain.RepoEntry;
import de.is24.infrastructure.gridfs.http.domain.RepoType;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Date;
import java.util.List;
import java.util.Set;


/**
 * @see http://static.springsource.org/spring-data/data-mongodb/docs/current/reference/html/repositories.html#repositories.query-methods
 * @see http://static.springsource.org/spring-data/data-mongodb/docs/current/reference/html/repository-query-keywords.html
 */
public interface RepoEntriesRepository extends MongoRepository<RepoEntry, ObjectId> {
  List<RepoEntry> findByName(String reponame);

  List<RepoEntry> findByType(RepoType type);

  List<RepoEntry> findByTypeIn(RepoType... types);

  List<RepoEntry> findByType(RepoType type, Sort sort);

  List<RepoEntry> findByTypeAndNameStartsWith(RepoType type, String reponame);

  List<RepoEntry> findByTypeInAndNameStartsWithAndTagsContainsAndLastModifiedIsBetween(Set<RepoType> repoTypes,
                                                                                       String name, String tag,
                                                                                       Date newerDate, Date olderDate);

  List<RepoEntry> findByTypeInAndNameStartsWithAndLastModifiedIsBetween(Set<RepoType> repoTypes, String name,
                                                                        Date newerDate, Date olderDate);

  RepoEntry findFirstByName(String reponame);

  RepoEntry findFirstByNameAndType(String reponame, RepoType type);
}
