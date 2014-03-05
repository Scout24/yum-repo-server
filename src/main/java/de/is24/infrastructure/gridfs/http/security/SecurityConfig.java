package de.is24.infrastructure.gridfs.http.security;

import de.is24.infrastructure.gridfs.http.utils.HostnameResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import static de.is24.infrastructure.gridfs.http.security.WhiteListAuthenticationFilter.WHITE_LISTED_HOSTS_MODIFCATION_ENABLED_KEY;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  public static final String ROLE_USER = "USER";

  @Autowired
  WhiteListAuthenticationProvider whiteListAuthenticationProvider;

  @Autowired
  @Qualifier("pamAuthenticationProvider")
  AuthenticationProvider pamAuthenticationProvider;

  @Value("${security.whitelist.hosts:}")
  String whiteListedHosts;

  @Value("${" + WHITE_LISTED_HOSTS_MODIFCATION_ENABLED_KEY + ":false}")
  boolean whiteListModificationEnabled;

  @Autowired
  HostnameResolver hostnameResolver;

  public SecurityConfig() {
    super(true);
  }

  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth.authenticationProvider(whiteListAuthenticationProvider)
        .authenticationProvider(pamAuthenticationProvider);
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.exceptionHandling()
        .and().httpBasic()
        .and().headers()
        .and().securityContext()
        .and().anonymous()
        .and().servletApi()
        .and().sessionManagement()
        .and().authorizeRequests()
        .antMatchers(POST, "/**").hasRole(ROLE_USER)
        .antMatchers(PUT, "/**").hasRole(ROLE_USER)
        .antMatchers(DELETE, "/**").hasRole(ROLE_USER)
        .and().addFilter(whiteListAuthenticationFilter());
  }

  @Bean(name="authenticationManager")
  @Override
  public AuthenticationManager authenticationManagerBean() throws Exception {
    return super.authenticationManagerBean();
  }

  @Bean
  public WhiteListAuthenticationFilter whiteListAuthenticationFilter() throws Exception {
    return new WhiteListAuthenticationFilter(whiteListedHosts, whiteListModificationEnabled, authenticationManagerBean(), hostnameResolver);
  }
}
