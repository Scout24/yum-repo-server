package de.is24.infrastructure.gridfs.http.security;

import de.is24.infrastructure.gridfs.http.gridfs.GridFsFileDescriptor;
import de.is24.infrastructure.gridfs.http.gridfs.GridFsService;
import de.is24.infrastructure.gridfs.http.utils.HostName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;

import static de.is24.infrastructure.gridfs.http.security.HostNameFilter.REMOTE_HOST_NAME;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {SecurityTestConfig.class})
@ActiveProfiles("test")
public class MethodSecurityConfigTest {

  public static final String PROTECTED_REPO = "protected-repo";
  public static final String ARCH = "arch";
  public static final String TEST_FILENAME = "test.rpm";
  public static final GridFsFileDescriptor RPM_FILE_IN_PROTECTED_REPO = new GridFsFileDescriptor(PROTECTED_REPO, ARCH, TEST_FILENAME);
  public static final String ANOTHER_REPO = "another-repo";
  @Autowired
  private GridFsService gridFsService;

  @Before
  public void setUp() throws Exception {
    AnonymousAuthenticationToken authentication = new AnonymousAuthenticationToken("key", "anonymousUser", createAuthorityList("ROLE_ANONYMOUS"));
    SecurityContextHolder.getContext().setAuthentication(authentication);
    MockHttpServletRequest servletRequest = new MockHttpServletRequest();
    ServletWebRequest attributes = new ServletWebRequest(servletRequest);
    attributes.setAttribute(REMOTE_HOST_NAME, new HostName("foobar"), SCOPE_REQUEST);
    RequestContextHolder.setRequestAttributes(attributes);
  }

  @Test(expected=AccessDeniedException.class)
  public void forbidAccessToGridFsFile() throws Exception {
    gridFsService.findFileByDescriptor(RPM_FILE_IN_PROTECTED_REPO);
  }

  @Test(expected=AccessDeniedException.class)
  public void forbidAccessToGridFsResource() throws Exception {
    gridFsService.getResource(RPM_FILE_IN_PROTECTED_REPO);
  }

  @Test(expected=AccessDeniedException.class)
  public void forbidPropagtionOfProtectedRpms() throws Exception {
    gridFsService.propagateRpm(RPM_FILE_IN_PROTECTED_REPO.getPath(), ANOTHER_REPO);
  }

  @Test(expected=AccessDeniedException.class)
  public void forbidPropagtionOfProtectedRepos() throws Exception {
    gridFsService.propagateRepository(PROTECTED_REPO, ANOTHER_REPO);
  }
}
