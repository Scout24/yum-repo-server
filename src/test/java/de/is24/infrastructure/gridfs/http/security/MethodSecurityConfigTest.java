package de.is24.infrastructure.gridfs.http.security;

import de.is24.infrastructure.gridfs.http.gridfs.StorageService;
import de.is24.infrastructure.gridfs.http.storage.FileDescriptor;
import de.is24.infrastructure.gridfs.http.storage.FileStorageService;
import de.is24.infrastructure.gridfs.http.utils.HostName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {SecurityTestConfig.class})
@ActiveProfiles("test")
public class MethodSecurityConfigTest {

  public static final String PROTECTED_REPO = "protected-repo";
  public static final String ARCH = "arch";
  public static final String TEST_FILENAME = "test.rpm";
  public static final FileDescriptor RPM_FILE_IN_PROTECTED_REPO = new FileDescriptor(PROTECTED_REPO, ARCH, TEST_FILENAME);
  public static final String ANOTHER_REPO = "another-repo";

  @Autowired
  private StorageService storageService;

  @Autowired
  private FileStorageService fileStorageService;

  @Before
  public void setUp() throws Exception {
    AnonymousAuthenticationToken authentication = new AnonymousAuthenticationToken("key", "anonymousUser", createAuthorityList("ROLE_ANONYMOUS"));
    authentication.setDetails(new AuthenticationDetails(new HostName("foobar")));
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  @Test(expected=AccessDeniedException.class)
  public void forbidAccessToGridFsFile() throws Exception {
    fileStorageService.findBy(RPM_FILE_IN_PROTECTED_REPO);
  }

  @Test(expected=AccessDeniedException.class)
  public void forbidAccessToGridFsResource() throws Exception {
    fileStorageService.getResource(RPM_FILE_IN_PROTECTED_REPO);
  }

  @Test(expected=AccessDeniedException.class)
  public void forbidPropagtionOfProtectedRpms() throws Exception {
    storageService.propagateRpm(RPM_FILE_IN_PROTECTED_REPO.getPath(), ANOTHER_REPO);
  }

  @Test(expected=AccessDeniedException.class)
  public void forbidPropagtionOfProtectedRepos() throws Exception {
    storageService.propagateRepository(PROTECTED_REPO, ANOTHER_REPO);
  }
}
