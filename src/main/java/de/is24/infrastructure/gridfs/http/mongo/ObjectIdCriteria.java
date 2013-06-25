package de.is24.infrastructure.gridfs.http.mongo;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;


public final class ObjectIdCriteria extends Criteria {
  private ObjectIdCriteria() {
    super("_id");
  }

  public static Criteria whereObjectIdIs(final ObjectId id) {
    return new ObjectIdCriteria().is(id);
  }
}
