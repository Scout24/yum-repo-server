package de.is24.infrastructure.gridfs.http.rpm;

import de.is24.infrastructure.gridfs.http.exception.InvalidRpmHeaderException;
import java.util.HashMap;
import java.util.Map;
import static org.freecompany.redline.header.Flags.PREREQ;
import static org.freecompany.redline.header.Flags.SCRIPT_POST;
import static org.freecompany.redline.header.Flags.SCRIPT_PRE;


public final class RpmPROTagFlagsToYumConverter {
  private RpmPROTagFlagsToYumConverter() {
  }

  private static final Map<Integer, String> FLAG_TO_STRING_MAP = new HashMap<Integer, String>() {
    {
      put(0, null);
      put(2, "LT");
      put(4, "GT");
      put(8, "EQ");
      put(10, "LE");
      put(12, "GE");
    }
  };

  public static String convert(final int flag) throws InvalidRpmHeaderException {
    int fixedFlag = fixFlag(flag);
    if (FLAG_TO_STRING_MAP.containsKey(fixedFlag)) {
      return FLAG_TO_STRING_MAP.get(fixedFlag);
    }
    throw new InvalidRpmHeaderException("Unsupported flag for provide/require/obsolete flag: " + flag);
  }

  public static boolean isPre(final int flag) {
    return (flag & (PREREQ | SCRIPT_PRE | SCRIPT_POST)) > 0;
  }

  private static int fixFlag(final int flag) {
    return flag & 0xf;
  }
}
