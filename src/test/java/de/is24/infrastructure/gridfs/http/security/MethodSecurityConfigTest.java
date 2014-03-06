package de.is24.infrastructure.gridfs.http.security;

import de.is24.infrastructure.gridfs.http.gridfs.GridFsFileDescriptor;
import de.is24.infrastructure.gridfs.http.gridfs.GridFsService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
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

  @Autowired
  private GridFsService gridFsService;

  @Test
  public void shouldPreventAccessToGridFsFile() throws Exception {
    AnonymousAuthenticationToken authentication = new AnonymousAuthenticationToken("key", "anonymousUser", createAuthorityList("ROLE_ANONYMOUS"));
    SecurityContextHolder.getContext().setAuthentication(authentication);
    gridFsService.findFileByDescriptor(new GridFsFileDescriptor("protected-repo", "noarch", "test.rpm"));
  }
}
