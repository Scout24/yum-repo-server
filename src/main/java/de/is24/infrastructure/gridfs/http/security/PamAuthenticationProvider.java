package de.is24.infrastructure.gridfs.http.security;

import org.jvnet.libpam.PAM;
import org.jvnet.libpam.PAMException;
import org.jvnet.libpam.UnixUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import static de.is24.infrastructure.gridfs.http.Profiles.PROD;
import static de.is24.infrastructure.gridfs.http.security.UserAuthorities.USER_AUTHORITIES;

@Component
@Profile(PROD)
public class PamAuthenticationProvider implements AuthenticationProvider {

  private final String pamServiceName;

  @Autowired
  public PamAuthenticationProvider(@Value("${pam.service.name:password-auth}") String pamServiceName) {
    this.pamServiceName = pamServiceName;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    String username = String.valueOf(authentication.getPrincipal());
    String password = String.valueOf(authentication.getCredentials());
    PAM pam;
    try {
      pam = new PAM(pamServiceName);
    } catch (PAMException e) {
      throw new AuthenticationServiceException("Could not initialize PAM.", e);
    }
    try {
      UnixUser user = pam.authenticate(username, password);
      return new UsernamePasswordAuthenticationToken(user.getUserName(), authentication.getCredentials(), USER_AUTHORITIES);
    } catch (PAMException e) {
      throw new BadCredentialsException("PAM authentication failed.", e);
    } finally {
      pam.dispose();
    }
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }
}
