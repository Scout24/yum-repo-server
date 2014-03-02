package de.is24.infrastructure.gridfs.http.utils;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class WildcardToRegexConverterTest {

  @Test
  public void matchEmptyPattern() throws Exception {
    assertMatch("", "");
  }

  @Test
  public void matchWihoutWildcard() throws Exception {
    assertMatch("foo.bar", "foo.bar");
    assertNotMatch("foo.bar", "foo2bar");
    assertNotMatch("bar", "foobar");
    assertNotMatch("bar", "barfoo");
    assertNotMatch("foobar", "any");
    assertNotMatch("any", "foobar");
  }

  @Test
  public void matchWildcard() throws Exception {
    assertMatch("*bar", "foobar");
    assertNotMatch("*bar", "foobar23");
    assertMatch("${..dsf*sd.ffsdf*]|}", "${..dsffoosd.ffsdfbar]|}");
    assertNotMatch("*bar", "foo.bar");
  }

  private void assertNotMatch(String pattern, String string) {
    assertMatch(pattern, string, false);
  }

  private void assertMatch(String pattern, String string) {
    assertMatch(pattern, string, true);
  }

  private void assertMatch(String pattern, String string, boolean expected) {
    assertThat(new WildcardToRegexConverter().convert(pattern).matcher(string).matches(), is(expected));
  }
}
