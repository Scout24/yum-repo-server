package de.is24.infrastructure.gridfs.http.metadata;

import de.is24.infrastructure.gridfs.http.domain.YumEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.REPO_KEY;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Service
public class YumEntriesRepositoryImpl implements DeleteByRepoYumEntries {

  private final MongoTemplate mongoTemplate;

  @Autowired
  public YumEntriesRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public void deleteByRepo(String reponame) {
    mongoTemplate.remove(query(where(REPO_KEY).is(reponame)), YumEntry.class);
  }
}
