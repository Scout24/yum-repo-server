package de.is24.infrastructure.gridfs.http.security;

import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import static de.is24.infrastructure.gridfs.http.Profiles.DEV;
import static de.is24.infrastructure.gridfs.http.security.UserAuthorities.USER_AUTHORITIES;

@Component("pamAuthenticationProvider")
@Profile(DEV)
public class DevUsernameEqualsPasswordAuthenticationProvider implements AuthenticationProvider {

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    if (authentication.getPrincipal().equals(authentication.getCredentials())) {
      return new UsernamePasswordAuthenticationToken(authentication.getPrincipal(), authentication.getCredentials(), USER_AUTHORITIES);
    }

    return null;
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }
}
