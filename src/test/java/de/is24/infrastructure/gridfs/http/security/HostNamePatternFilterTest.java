package de.is24.infrastructure.gridfs.http.security;

import de.is24.infrastructure.gridfs.http.gridfs.GridFsFileDescriptor;
import de.is24.infrastructure.gridfs.http.utils.HostName;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


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
    givenRequestForHost("devabc01");

    GridFsFileDescriptor gridFsFileDescriptor = new GridFsFileDescriptor(NOT_PROTECTED_REPO, "noarch",
      "lala-devxyz01.noarch.rpm");

    boolean allowed = patternFilter.isAllowed(gridFsFileDescriptor);

    assertThat(allowed, is(true));
  }

  @Test
  public void allowAccessToMetadataFilesForAnyHostInProtectedRepos() throws Exception {
    givenRequestForHost("devabc01");

    boolean allowed = patternFilter.isAllowed(METADATA_IN_PROTECTED_REPO_DESCRIPTOR);

    assertThat(allowed, is(true));
  }

  @Test
  public void allowAccessToMetadataFilesForHostGivenByIPInProtectedRepos() throws Exception {
    givenRequestForHost(NOT_WHITELISTED_IP);

    boolean allowed = patternFilter.isAllowed(METADATA_IN_PROTECTED_REPO_DESCRIPTOR);

    assertThat(allowed, is(true));
  }


  @Test
  public void allowAccessToFilesContainingHostnameInProtectedRepos() throws Exception {
    givenRequestForHost("devxyz01");

    boolean allowed = patternFilter.isAllowed(PROTECTED_NOARCH_RPM_FOR_DEVXYZ01_DESCRIPTOR);

    assertThat(allowed, is(true));
  }

  @Test
  public void allowAccessToFilesContainingHostnameWithoutDomainPartInProtectedRepos() throws Exception {
    givenRequestForHost("devxyz01.rz.is");

    boolean allowed = patternFilter.isAllowed(PROTECTED_NOARCH_RPM_FOR_DEVXYZ01_DESCRIPTOR);

    assertThat(allowed, is(true));
  }

  @Test
  public void denyAccessToFilesNotContainingHostnameInProtectedRepos() throws Exception {
    givenRequestForHost("devabc01");

    boolean allowed = patternFilter.isAllowed(PROTECTED_NOARCH_RPM_FOR_DEVXYZ01_DESCRIPTOR);

    assertThat(allowed, is(false));
  }

  @Test
  public void denyAccessToFilesForIPOnlyHostnamesIfNoWhiteListIsGiven() throws Exception {
    givenRequestForHost(WHITELISTED_IP);

    boolean allowed = patternFilter.isAllowed(PROTECTED_NOARCH_RPM_FOR_DEVXYZ01_DESCRIPTOR);

    assertThat(allowed, is(false));
  }


  @Test
  public void denyAccessToFilesForIPOnlyHostnamesNotInWhiteListInProtectedRepos() throws Exception {
    givenPatternFilterWithWhitelist();
    givenRequestForHost(NOT_WHITELISTED_IP);

    boolean allowed = patternFilter.isAllowed(PROTECTED_NOARCH_RPM_FOR_DEVXYZ01_DESCRIPTOR);

    assertThat(allowed, is(false));
  }

  @Test
  public void allowAccessToFilesForWhiteListedIPOnlyHostnamesInProtectedRepos() throws Exception {
    givenPatternFilterWithWhitelist();
    givenRequestForHost(WHITELISTED_IP);

    boolean allowed = patternFilter.isAllowed(PROTECTED_NOARCH_RPM_FOR_DEVXYZ01_DESCRIPTOR);

    assertThat(allowed, is(true));
  }


  @Test
  public void allowAccessToFilesForInternalCallsInProtectedRepos() throws Exception {
    // internal call = no Request Context in RequestContextHolder

    boolean allowed = patternFilter.isAllowed(PROTECTED_NOARCH_RPM_FOR_DEVXYZ01_DESCRIPTOR);

    assertThat(allowed, is(true));
  }


  private void givenRequestForHost(String hostname) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute(HostNameFilter.REMOTE_HOST_NAME, new HostName(hostname));
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  }

  private void givenPatternFilterWithWhitelist() {
    patternFilter = new HostNamePatternFilter(PROTECTED_REPO, "11.11.11.1-254");
  }
}
