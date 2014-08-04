package de.is24.infrastructure.gridfs.http.security;

import de.is24.infrastructure.gridfs.http.storage.FileDescriptor;
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


public class ProtectedRepoAccessEvaluatorTest {
  public static final String PROTECTED_REPO = "protected";
  public static final String NOT_PROTECTED_REPO = "notProtected";

  public static final String WHITELISTED_IP = "11.11.11.11";
  public static final String NOT_WHITELISTED_IP = "1.2.3.4";
  private ProtectedRepoAccessEvaluator patternEvaluator;

  public static final FileDescriptor PROTECTED_NOARCH_RPM_FOR_DEVXYZ01_DESCRIPTOR = new FileDescriptor(
    PROTECTED_REPO,
    "noarch",
    "lala-devxyz01.noarch.rpm");
  public static final FileDescriptor METADATA_IN_PROTECTED_REPO_DESCRIPTOR = new FileDescriptor(
    PROTECTED_REPO, "repodata", "repomd.xml");

  @Before
  public void setup() {
    patternEvaluator = new ProtectedRepoAccessEvaluator(PROTECTED_REPO, "");
  }

  @After
  public void cleanup() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  public void allowAccessToAnyFileForAnyHostToReposNotProtected() throws Exception {
    FileDescriptor fileDescriptor = new FileDescriptor(NOT_PROTECTED_REPO, "noarch",
      "lala-devxyz01.noarch.rpm");

    boolean allowed = patternEvaluator.isAllowed(fileDescriptor, currentAuthentication("devabc01"));

    assertThat(allowed, is(true));
  }

  @Test
  public void allowAccessToMetadataFilesForAnyHostInProtectedRepos() throws Exception {
    boolean allowed = patternEvaluator.isAllowed(METADATA_IN_PROTECTED_REPO_DESCRIPTOR, currentAuthentication("devabc01"));

    assertThat(allowed, is(true));
  }

  @Test
  public void allowAccessToMetadataFilesForHostGivenByIPInProtectedRepos() throws Exception {
    boolean allowed = patternEvaluator.isAllowed(METADATA_IN_PROTECTED_REPO_DESCRIPTOR, currentAuthentication(NOT_WHITELISTED_IP));

    assertThat(allowed, is(true));
  }

  @Test
  public void allowAccessToFilesContainingHostnameInProtectedRepos() throws Exception {
    boolean allowed = patternEvaluator.isAllowed(PROTECTED_NOARCH_RPM_FOR_DEVXYZ01_DESCRIPTOR, currentAuthentication("devxyz01"));

    assertThat(allowed, is(true));
  }


  @Test
  public void allowAccessToFilesContainingHostnameWithoutDomainPartInProtectedRepos() throws Exception {
    boolean allowed = patternEvaluator.isAllowed(PROTECTED_NOARCH_RPM_FOR_DEVXYZ01_DESCRIPTOR, currentAuthentication("devxyz01.rz.is"));

    assertThat(allowed, is(true));
  }

  @Test
  public void denyAccessToFilesNotContainingHostnameInProtectedRepos() throws Exception {
    boolean allowed = patternEvaluator.isAllowed(PROTECTED_NOARCH_RPM_FOR_DEVXYZ01_DESCRIPTOR, currentAuthentication("devabc01"));

    assertThat(allowed, is(false));
  }

  @Test
  public void denyAccessToFilesForIPOnlyHostnamesIfNoWhiteListIsGiven() throws Exception {
    boolean allowed = patternEvaluator.isAllowed(PROTECTED_NOARCH_RPM_FOR_DEVXYZ01_DESCRIPTOR, currentAuthentication(WHITELISTED_IP));

    assertThat(allowed, is(false));
  }

  @Test
  public void denyAccessToFilesForIPOnlyHostnamesNotInWhiteListInProtectedRepos() throws Exception {
    givenPatternEvaluatorWithWhitelist();

    boolean allowed = patternEvaluator.isAllowed(PROTECTED_NOARCH_RPM_FOR_DEVXYZ01_DESCRIPTOR, currentAuthentication(NOT_WHITELISTED_IP));

    assertThat(allowed, is(false));
  }


  @Test
  public void allowAccessToFilesForWhiteListedIPOnlyHostnamesInProtectedRepos() throws Exception {
    givenPatternEvaluatorWithWhitelist();
    boolean allowed = patternEvaluator.isAllowed(PROTECTED_NOARCH_RPM_FOR_DEVXYZ01_DESCRIPTOR, currentAuthentication(WHITELISTED_IP));
    assertThat(allowed, is(true));
  }

  @Test
  public void allowAccessToFilesForInternalCallsInProtectedRepos() throws Exception {
    boolean allowed = patternEvaluator.isAllowed(PROTECTED_NOARCH_RPM_FOR_DEVXYZ01_DESCRIPTOR, null);
    assertThat(allowed, is(true));
  }


  private Authentication currentAuthentication(String hostname) {
    AnonymousAuthenticationToken authentication = new AnonymousAuthenticationToken("key", "anonymousUser", createAuthorityList("ROLE_ANONYMOUS"));
    authentication.setDetails(new AuthenticationDetails(new HostName(hostname)));
    return authentication;
  }

  private void givenPatternEvaluatorWithWhitelist() {
    patternEvaluator = new ProtectedRepoAccessEvaluator(PROTECTED_REPO, "11.11.11.1-254");
  }
}
