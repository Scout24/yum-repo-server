package de.is24.infrastructure.gridfs.http.rpm.version;

import com.mongodb.DBObject;
import java.util.Comparator;
import java.util.List;


public class CachingVersionDBObjectComparator implements Comparator<Object> {
  private enum VersionField {
    epoch,
    ver,
    rel;

    private final String cacheField;

    private VersionField() {
      cacheField = name() + "-segments";
    }

    private String getCacheField() {
      return cacheField;
    }
  }

  private RpmVersionSegmentComparator segmentComparator = new RpmVersionSegmentComparator();
  private RpmVersionSegmentGenerator segmentGenerator = new RpmVersionSegmentGenerator();


  @Override
  public int compare(Object o1, Object o2) {
    if (o1 == null) {
      return (o2 == null) ? 0 : -1;
    }
    if (o2 == null) {
      return 1;
    }
    if ((o1 instanceof DBObject) && (o2 instanceof DBObject)) {
      return compareBothInstances((DBObject) o1, (DBObject) o2);
    } else {
      throw new IllegalArgumentException("Could compare DBObjects only");
    }
  }

  private int compareBothInstances(final DBObject dbo1, final DBObject dbo2) {
    int res = compareVersionFieldEpoch(dbo1, dbo2);
    if (res != 0) {
      return res;
    }

    res = compareVersionFieldVer(dbo1, dbo2);
    if (res != 0) {
      return res;
    }

    return compareVersionFieldRel(dbo1, dbo2);
  }

  private int compareVersionFieldRel(final DBObject dbo1, final DBObject dbo2) {
    return segmentComparator.compare(cacheSegments(dbo1, VersionField.rel), cacheSegments(dbo2, VersionField.rel));
  }

  private int compareVersionFieldVer(final DBObject dbo1, final DBObject dbo2) {
    return segmentComparator.compare(cacheSegments(dbo1, VersionField.ver), cacheSegments(dbo2, VersionField.ver));
  }

  private int compareVersionFieldEpoch(final DBObject dbo1, final DBObject dbo2) {
    return Integer.compare((int) dbo1.get(VersionField.epoch.name()), (int) dbo2.get(VersionField.epoch.name()));
  }

  private List<String> cacheSegments(DBObject obj, VersionField field) {
    if (!obj.containsField(field.getCacheField())) {
      obj.put(field.getCacheField(), segmentGenerator.generate((String) obj.get(field.name())));
    }

    return (List<String>) obj.get(field.getCacheField());
  }
}
