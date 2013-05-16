package de.is24.infrastructure.gridfs.http.rpm.version;

import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageVersion;
import java.util.Comparator;
import java.util.List;


public class YumPackageVersionComparator implements Comparator<YumPackageVersion> {
  private RpmVersionSegmentComparator segmentComparator = new RpmVersionSegmentComparator();
  private RpmVersionSegmentGenerator segmentGenerator = new RpmVersionSegmentGenerator();

  @Override
  public int compare(YumPackageVersion o1, YumPackageVersion o2) {
    if (o1 == null) {
      return (o2 == null) ? 0 : -1;
    }
    if (o2 == null) {
      return 1;
    }

    int res = Integer.compare(o1.getEpoch(), o2.getEpoch());
    if (res != 0) {
      return res;
    }

    res = segmentComparator.compare(segments(o1.getVer()), segments(o2.getVer()));
    if (res != 0) {
      return res;
    }

    return segmentComparator.compare(segments(o1.getRel()), segments(o2.getRel()));
  }

  private List<String> segments(String version) {
    return segmentGenerator.generate(version);
  }
}
