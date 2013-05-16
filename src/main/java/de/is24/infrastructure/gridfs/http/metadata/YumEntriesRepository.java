package de.is24.infrastructure.gridfs.http.metadata;

import de.is24.infrastructure.gridfs.http.domain.YumEntry;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;


public interface YumEntriesRepository extends MongoRepository<YumEntry, ObjectId> {
  List<YumEntry> findByRepo(String reponame);

  List<YumEntry> findByRepoAndYumPackageName(String reponame, String rpmName);

  List<YumEntry> findByRepoAndYumPackageArchAndYumPackageName(String reponame, String arch, String rpmName);

  List<YumEntry> findByRepoAndYumPackageLocationHref(String repo, String location);
}
