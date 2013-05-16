package de.is24.infrastructure.gridfs.http.rpm;

import de.is24.infrastructure.gridfs.http.domain.yum.YumPackageChangeLog;
import de.is24.infrastructure.gridfs.http.exception.InvalidRpmHeaderException;
import org.freecompany.redline.header.Header;
import java.util.ArrayList;
import java.util.List;


public class RpmToYumChangeLogConverter {
  private final RpmHeaderWrapper rpmHeaderWrapper;

  public RpmToYumChangeLogConverter(final RpmHeaderWrapper rpmHeaderWrapper) {
    this.rpmHeaderWrapper = rpmHeaderWrapper;
  }

  public List<YumPackageChangeLog> convert() throws InvalidRpmHeaderException {
    return createEntries(
      rpmHeaderWrapper.readStrings(Header.HeaderTag.CHANGELOGNAME, false),
      rpmHeaderWrapper.readIntegers(Header.HeaderTag.CHANGELOGTIME, false),
      rpmHeaderWrapper.readStrings(Header.HeaderTag.CHANGELOGTEXT, false));
  }

  private List<YumPackageChangeLog> createEntries(final String[] authors, final int[] dates, final String[] messages) {
    final List<YumPackageChangeLog> entries = new ArrayList<>();
    for (int i = 0; i < authors.length; i++) {
      entries.add(createYumChangeLog(authors[i], dates[i], messages[i]));
    }
    return entries;
  }

  private YumPackageChangeLog createYumChangeLog(final String author, final int date, final String message) {
    final YumPackageChangeLog changeLog = new YumPackageChangeLog();
    changeLog.setAuthor(author);
    changeLog.setDate(date);
    changeLog.setMessage(message);
    return changeLog;
  }
}
