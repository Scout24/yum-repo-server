package de.is24.infrastructure.gridfs.http.metadata;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.DocumentCallbackHandler;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import java.security.MessageDigest;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.REPO_KEY;
import static de.is24.infrastructure.gridfs.http.mongo.DatabaseStructure.YUM_ENTRY_COLLECTION;
import static org.apache.commons.codec.binary.StringUtils.getBytesUtf8;
import static org.apache.commons.codec.digest.DigestUtils.getMd5Digest;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;


@Service
public class YumEntriesHashCalculator {
  private final MongoTemplate mongoTemplate;

  @Autowired
  public YumEntriesHashCalculator(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  public String hashForRepo(final String reponame) {
    final Query query = query(where(REPO_KEY).is(reponame));
    query.with(new Sort("_id")).fields().include("yumPackage.checksum.checksum");


    final Md5CalcDocumentCallbackHandler md5CalcCallback = new Md5CalcDocumentCallbackHandler();
    mongoTemplate.executeQuery(query, YUM_ENTRY_COLLECTION, md5CalcCallback);

    return md5CalcCallback.md5HexString();
  }


  private static class Md5CalcDocumentCallbackHandler implements DocumentCallbackHandler {
    private MessageDigest md5Digest = getMd5Digest();
    private boolean hasContent;

    @Override
    public void processDocument(DBObject dbObject) throws MongoException, DataAccessException {
      final String singleCheckSum = getCheckSum(dbObject);
      md5Digest.update(getBytesUtf8(singleCheckSum));
      hasContent = true;
    }

    String md5HexString() {
      if (hasContent) {
        return Hex.encodeHexString(md5Digest.digest());
      }
      return EMPTY;
    }

    private static String getCheckSum(DBObject dbObject) {
      return ((BasicDBObject) ((BasicDBObject) dbObject.get("yumPackage")).get("checksum")).getString("checksum");
    }
  }

}
