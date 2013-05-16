package de.is24.infrastructure.gridfs.http.rpm.version;

import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class RpmVersionSegmentGeneratorTest {

  private final RpmVersionSegmentGenerator generator = new RpmVersionSegmentGenerator();

  @Test
  public void generateSegments() throws Exception {
    assertSegments("1.0.1", "1", "0", "1");
    assertSegments("1.0abc.1ghi56", "1", "0", "abc", "1", "ghi", "56");
    assertSegments("0123.abc.gh56", "0123", "abc", "gh", "56");
    assertSegments("0....abc...5", "0", "", "", "", "abc", "", "", "5");
    assertThat(generator.generate(null), nullValue());
    assertThat(generator.generate(""), nullValue());
  }

  private void assertSegments(String version, String... expectedSegments) {
    List<String> segments = generator.generate(version);
    assertThat(segments, is(asList(expectedSegments)));
  }
}
