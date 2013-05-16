package de.is24.infrastructure.gridfs.http.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import static de.is24.infrastructure.gridfs.http.security.UserAuthorities.USER_AUTHORITIES;

@Component
public class WhiteListAuthenticationProvider implements AuthenticationProvider {
  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    if (supports(authentication.getClass())) {
      return new PreAuthenticatedAuthenticationToken(authentication.getPrincipal(), authentication.getCredentials(), USER_AUTHORITIES);
    }
    return null;
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return PreAuthenticatedAuthenticationToken.class.isAssignableFrom(authentication);
  }
}
