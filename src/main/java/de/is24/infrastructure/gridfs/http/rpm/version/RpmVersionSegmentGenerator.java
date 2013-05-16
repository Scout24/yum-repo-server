package de.is24.infrastructure.gridfs.http.rpm.version;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static java.lang.Character.isDigit;
import static java.util.regex.Pattern.compile;

public class RpmVersionSegmentGenerator {

  private static final Pattern NOT_ALPHANUM = compile("[^a-zA-Z0-9]");

  public List<String> generate(String s) {
    if (s == null || s.length() == 0) {
      return null;
    }

    String[] alphaNumericParts = NOT_ALPHANUM.split(s);
    List<String> segments = new ArrayList<>(alphaNumericParts.length);
    for (String alphaNumericPart : alphaNumericParts) {
      boolean digit = false;
      StringBuilder segment = new StringBuilder();
      for (int i = 0; i < alphaNumericPart.length(); i++) {
        char currentChar = alphaNumericPart.charAt(i);
        boolean currentCharDigit = isDigit(currentChar);

        if (segment.length() == 0) {
          segment.append(currentChar);
          digit = currentCharDigit;
          continue;
        }

        if (digit == currentCharDigit) {
          segment.append(currentChar);
          continue;
        }

        segments.add(segment.toString());
        segment = new StringBuilder();
        segment.append(currentChar);
        digit = currentCharDigit;
      }

      segments.add(segment.toString());
    }

    return segments;
  }
}
