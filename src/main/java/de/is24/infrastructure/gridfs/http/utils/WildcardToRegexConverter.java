package de.is24.infrastructure.gridfs.http.utils;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

public class WildcardToRegexConverter {

  public Pattern convert(String pattern) {
    String convertedPattern = pattern.replaceAll("([\\.\\$\\|\\(\\)\\[\\{\\^\\?\\+\\\\])", "\\\\$1").replaceAll("\\*", "[\\\\w\\-_]*");
    return compile(convertedPattern);
  }
}
