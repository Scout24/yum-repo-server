package de.is24.infrastructure.gridfs.http.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.ArrayList;
import java.util.List;


public final class UserAuthorities {
  public static final List<GrantedAuthority> USER_AUTHORITIES = new ArrayList<>();

  private UserAuthorities() {
  }

  static {
    USER_AUTHORITIES.add(new SimpleGrantedAuthority("ROLE_USER"));
  }
}
