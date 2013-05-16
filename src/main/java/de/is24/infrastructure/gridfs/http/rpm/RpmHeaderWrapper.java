package de.is24.infrastructure.gridfs.http.rpm;

import de.is24.infrastructure.gridfs.http.exception.InvalidRpmHeaderException;
import org.freecompany.redline.header.AbstractHeader;
import org.freecompany.redline.header.Header;

public class RpmHeaderWrapper {

  private final Header header;

  public RpmHeaderWrapper(Header header) {
    this.header = header;
  }

  public String readString(Header.HeaderTag tag, boolean forceNotNull) throws InvalidRpmHeaderException {
    String[] values = readStrings(tag, forceNotNull);
    return values.length > 0 ? values[0] : null;
  }

  public Integer readInteger(Header.HeaderTag tag, boolean forceNotNull) throws InvalidRpmHeaderException {
    int[] values = readIntegers(tag, forceNotNull);
    return values.length > 0 ? values[0] : 0;
  }

  public int[] readIntegers(Header.HeaderTag tag, boolean forceNotNull) throws InvalidRpmHeaderException {
    final AbstractHeader.Entry<?> entry = readEntry(tag, forceNotNull);
    return null != entry ? ((int[]) entry.getValues()) : new int[0];
  }

  public String[] readStrings(Header.HeaderTag tag, boolean forceNotNull) throws InvalidRpmHeaderException {
    final AbstractHeader.Entry<?> entry = readEntry(tag, forceNotNull);
    return null != entry ? ((String[]) entry.getValues()) : new String[0];
  }

  public short[] readShorts(Header.HeaderTag tag, boolean forceNotNull) throws InvalidRpmHeaderException {
    final AbstractHeader.Entry<?> entry = readEntry(tag, forceNotNull);
    return null != entry ? ((short[]) entry.getValues()) : new short[0];
  }

  private AbstractHeader.Entry<?> readEntry(final Header.HeaderTag tag, final boolean forceNotNull) throws InvalidRpmHeaderException {
    final AbstractHeader.Entry<?> entry = header.getEntry(tag);

    if (entry == null && forceNotNull) {
      throw new InvalidRpmHeaderException("Header entry not found " + tag);
    }

    return entry;
  }

  public Header getHeader() {
    return header;
  }
}
