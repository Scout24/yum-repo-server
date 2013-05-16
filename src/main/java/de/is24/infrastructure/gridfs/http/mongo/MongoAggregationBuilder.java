package de.is24.infrastructure.gridfs.http.mongo;

import com.mongodb.BasicDBObject;
import de.is24.infrastructure.gridfs.http.domain.SortField;
import de.is24.infrastructure.gridfs.http.domain.SortOrder;
import org.springframework.data.mongodb.core.query.Criteria;
import java.util.HashMap;
import java.util.Map;


public class MongoAggregationBuilder {
  private BasicDBObject groupBy;
  private Map<String, BasicDBObject> aggregationFields = new HashMap<String, BasicDBObject>();

  public static MongoAggregationBuilder groupBy(String id) {
    MongoAggregationBuilder builder = new MongoAggregationBuilder();
    builder.groupBy = new BasicDBObject("_id", ensureDollarPrefix(id));
    return builder;
  }

  public static MongoAggregationBuilder groupBy(Field... fields) {
    MongoAggregationBuilder builder = new MongoAggregationBuilder();
    builder.groupBy = new BasicDBObject("_id", toDbObject(fields));
    return builder;
  }

  public MongoAggregationBuilder sum(String field) {
    aggregationFields.put(field, new BasicDBObject("$sum", ensureDollarPrefix(field)));
    return this;
  }

  public MongoAggregationBuilder count() {
    aggregationFields.put("count", new BasicDBObject("$sum", 1));
    return this;
  }

  public MongoAggregationBuilder max(String field) {
    aggregationFields.put(field, new BasicDBObject("$max", ensureDollarPrefix(field)));
    return this;
  }

  public MongoAggregationBuilder push(String name, String value) {
    aggregationFields.put(name, new BasicDBObject("$push", ensureDollarPrefix(value)));
    return this;
  }

  public MongoAggregationBuilder push(String name, Field... fields) {
    aggregationFields.put(name, new BasicDBObject("$push", toDbObject(fields)));
    return this;
  }


  public BasicDBObject build() {
    for (Map.Entry<String, BasicDBObject> entry : aggregationFields.entrySet()) {
      groupBy.append(entry.getKey(), entry.getValue());
    }
    return new BasicDBObject().append("$group", groupBy);
  }

  public static BasicDBObject match(Criteria object) {
    return new BasicDBObject("$match", object.getCriteriaObject());
  }

  public static BasicDBObject sort(SortField sortBy, SortOrder sortOrder) {
    return new BasicDBObject("$sort", sortBy.sortFolder(sortOrder));
  }

  public static Field field(String name, String value) {
    return new Field(name, value);
  }

  private static Object toDbObject(Field... fields) {
    BasicDBObject object = new BasicDBObject();
    for (Field field : fields) {
      object.put(field.getName(), ensureDollarPrefix(field.getValue()));
    }
    return object;
  }

  private static String ensureDollarPrefix(String value) {
    return value.startsWith("$") ? value : ("$" + value);
  }

  public static class Field {
    private final String name;
    private final String value;

    public Field(String name, String value) {
      this.name = name;
      this.value = value;
    }

    public String getName() {
      return name;
    }

    public String getValue() {
      return value;
    }
  }

}
