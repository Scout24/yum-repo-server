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
  private HostNamePatternFilter patternFilter;

  @Before
  public void setup() {
    patternFilter = new HostNamePatternFilter(PROTECTED_REPO);
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

    GridFsFileDescriptor gridFsFileDescriptor = new GridFsFileDescriptor(PROTECTED_REPO, "repodata", "repomd.xml");

    boolean allowed = patternFilter.isAllowed(gridFsFileDescriptor);

    assertThat(allowed, is(true));
  }


  @Test
  public void allowAccessToFilesContainingHostnameInProtectedRepos() throws Exception {
    givenRequestForHost("devxyz01");

    GridFsFileDescriptor gridFsFileDescriptor = new GridFsFileDescriptor(PROTECTED_REPO, "noarch",
      "lala-devxyz01.noarch.rpm");

    boolean allowed = patternFilter.isAllowed(gridFsFileDescriptor);

    assertThat(allowed, is(true));
  }

  @Test
  public void allowAccessToFilesContainingHostnameWithoutDomainPartInProtectedRepos() throws Exception {
    givenRequestForHost("devxyz01.rz.is");

    GridFsFileDescriptor gridFsFileDescriptor = new GridFsFileDescriptor(PROTECTED_REPO, "noarch",
      "lala-devxyz01.noarch.rpm");

    boolean allowed = patternFilter.isAllowed(gridFsFileDescriptor);

    assertThat(allowed, is(true));
  }

  @Test
  public void denyAccessToFilesNotContainingHostnameInProtectedRepos() throws Exception {
    givenRequestForHost("devabc01");

    GridFsFileDescriptor gridFsFileDescriptor = new GridFsFileDescriptor(PROTECTED_REPO, "noarch",
      "lala-devxyz01.noarch.rpm");

    boolean allowed = patternFilter.isAllowed(gridFsFileDescriptor);

    assertThat(allowed, is(false));
  }

  @Test
  public void denyAccessToFilesForIPOnlyHostnamesInProtectedRepos() throws Exception {
    givenRequestForHost("1.2.3.4");

    GridFsFileDescriptor gridFsFileDescriptor = new GridFsFileDescriptor(PROTECTED_REPO, "noarch",
      "lala-devxyz01.noarch.rpm");

    boolean allowed = patternFilter.isAllowed(gridFsFileDescriptor);

    assertThat(allowed, is(false));
  }


  @Test
  public void allowAccessToFilesForInternalCallsInProtectedRepos() throws Exception {
    // internal call = no Request Context in RequestContextHolder
    GridFsFileDescriptor gridFsFileDescriptor = new GridFsFileDescriptor(PROTECTED_REPO, "noarch",
      "lala-devxyz01.noarch.rpm");

    boolean allowed = patternFilter.isAllowed(gridFsFileDescriptor);

    assertThat(allowed, is(true));
  }


  private void givenRequestForHost(String hostname) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute(HostNameFilter.REMOTE_HOST_NAME, new HostName(hostname));
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  }
}
