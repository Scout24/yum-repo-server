package de.is24.infrastructure.gridfs.http.mongo;

public class MongoStringToJsonConverter {

  public static final String REGEX_MATCHING_STRING_ARRAY_ELEMENT = "(\\[|,\\s*)([\\w:\\-\\.]*[a-zA-Z][\\w:\\-\\.]*)(,|\\])";
  public static final String STRING_ARRAY_ELEMENT_REPLACEMENT = "$1'$2'$3";

  public String convert(String input) {
    return replaceNumbersAndCurlyBracket(replaceArrayStart(replaceSimpleValues(replaceTypeNames(replaceStringArrays(input)))));
  }

  private String replaceNumbersAndCurlyBracket(String input) {
    return input.replaceAll("=(\\d+|\\{)", ":$1");
  }

  private String replaceArrayStart(String input) {
    return input.replaceAll("=([\\['])", ":$1");
  }

  private String replaceSimpleValues(String input) {
    return input.replaceAll("=([\\w:\\-\\.]*[a-zA-Z][\\w:\\-\\.]*)([,\\{])", ":'$1'$2");
  }

  private String replaceTypeNames(String input) {
    return input.replaceAll("\\w+\\{", "{");
  }

  private String replaceStringArrays(String input) {
    return input
        .replaceAll(REGEX_MATCHING_STRING_ARRAY_ELEMENT, STRING_ARRAY_ELEMENT_REPLACEMENT)
        .replaceAll(REGEX_MATCHING_STRING_ARRAY_ELEMENT, STRING_ARRAY_ELEMENT_REPLACEMENT);
  }
}
