package de.is24.infrastructure.gridfs.http.security;

import de.is24.infrastructure.gridfs.http.gridfs.GridFsFileDescriptor;
import de.is24.infrastructure.gridfs.http.utils.HostName;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.RequestContextHolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;


public class HostNamePatternFilterTest {
  public static final String PROTECTED_REPO = "protected";
  public static final String NOT_PROTECTED_REPO = "notProtected";

  public static final String WHITELISTED_IP = "11.11.11.11";
  public static final String NOT_WHITELISTED_IP = "1.2.3.4";
  private HostNamePatternFilter patternFilter;

  public static final GridFsFileDescriptor PROTECTED_NOARCH_RPM_FOR_DEVXYZ01_DESCRIPTOR = new GridFsFileDescriptor(
    PROTECTED_REPO,
    "noarch",
    "lala-devxyz01.noarch.rpm");
  public static final GridFsFileDescriptor METADATA_IN_PROTECTED_REPO_DESCRIPTOR = new GridFsFileDescriptor(
    PROTECTED_REPO, "repodata", "repomd.xml");

  @Before
  public void setup() {
    patternFilter = new HostNamePatternFilter(PROTECTED_REPO, "");
  }

  @After
  public void cleanup() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  public void allowAccessToAnyFileForAnyHostToReposNotProtected() throws Exception {
    GridFsFileDescriptor gridFsFileDescriptor = new GridFsFileDescriptor(NOT_PROTECTED_REPO, "noarch",
      "lala-devxyz01.noarch.rpm");

    boolean allowed = patternFilter.isAllowed(gridFsFileDescriptor, currentAuthentication("devabc01"));

    assertThat(allowed, is(true));
  }

  @Test
  public void allowAccessToMetadataFilesForAnyHostInProtectedRepos() throws Exception {
    boolean allowed = patternFilter.isAllowed(METADATA_IN_PROTECTED_REPO_DESCRIPTOR, currentAuthentication("devabc01"));

    assertThat(allowed, is(true));
  }

  @Test
  public void allowAccessToMetadataFilesForHostGivenByIPInProtectedRepos() throws Exception {
    boolean allowed = patternFilter.isAllowed(METADATA_IN_PROTECTED_REPO_DESCRIPTOR, currentAuthentication(NOT_WHITELISTED_IP));

    assertThat(allowed, is(true));
  }

  @Test
  public void allowAccessToFilesContainingHostnameInProtectedRepos() throws Exception {
    boolean allowed = patternFilter.isAllowed(PROTECTED_NOARCH_RPM_FOR_DEVXYZ01_DESCRIPTOR, currentAuthentication("devxyz01"));

    assertThat(allowed, is(true));
  }


  @Test
  public void allowAccessToFilesContainingHostnameWithoutDomainPartInProtectedRepos() throws Exception {
    boolean allowed = patternFilter.isAllowed(PROTECTED_NOARCH_RPM_FOR_DEVXYZ01_DESCRIPTOR, currentAuthentication("devxyz01.rz.is"));

    assertThat(allowed, is(true));
  }

  @Test
  public void denyAccessToFilesNotContainingHostnameInProtectedRepos() throws Exception {
    boolean allowed = patternFilter.isAllowed(PROTECTED_NOARCH_RPM_FOR_DEVXYZ01_DESCRIPTOR, currentAuthentication("devabc01"));

    assertThat(allowed, is(false));
  }

  @Test
  public void denyAccessToFilesForIPOnlyHostnamesIfNoWhiteListIsGiven() throws Exception {
    boolean allowed = patternFilter.isAllowed(PROTECTED_NOARCH_RPM_FOR_DEVXYZ01_DESCRIPTOR, currentAuthentication(WHITELISTED_IP));

    assertThat(allowed, is(false));
  }

  @Test
  public void denyAccessToFilesForIPOnlyHostnamesNotInWhiteListInProtectedRepos() throws Exception {
    givenPatternFilterWithWhitelist();

    boolean allowed = patternFilter.isAllowed(PROTECTED_NOARCH_RPM_FOR_DEVXYZ01_DESCRIPTOR, currentAuthentication(NOT_WHITELISTED_IP));

    assertThat(allowed, is(false));
  }


  @Test
  public void allowAccessToFilesForWhiteListedIPOnlyHostnamesInProtectedRepos() throws Exception {
    givenPatternFilterWithWhitelist();
    boolean allowed = patternFilter.isAllowed(PROTECTED_NOARCH_RPM_FOR_DEVXYZ01_DESCRIPTOR, currentAuthentication(WHITELISTED_IP));
    assertThat(allowed, is(true));
  }

  @Test
  public void allowAccessToFilesForInternalCallsInProtectedRepos() throws Exception {
    boolean allowed = patternFilter.isAllowed(PROTECTED_NOARCH_RPM_FOR_DEVXYZ01_DESCRIPTOR, null);
    assertThat(allowed, is(true));
  }


  private Authentication currentAuthentication(String hostname) {
    AnonymousAuthenticationToken authentication = new AnonymousAuthenticationToken("key", "anonymousUser", createAuthorityList("ROLE_ANONYMOUS"));
    authentication.setDetails(new AuthenticationDetails(new HostName(hostname)));
    return authentication;
  }

  private void givenPatternFilterWithWhitelist() {
    patternFilter = new HostNamePatternFilter(PROTECTED_REPO, "11.11.11.1-254");
  }
}
