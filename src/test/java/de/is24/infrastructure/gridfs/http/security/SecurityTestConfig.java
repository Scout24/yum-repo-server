package de.is24.infrastructure.gridfs.http.security;

import com.mongodb.DBCollection;
import de.is24.infrastructure.gridfs.http.gridfs.GridFsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
@Import({MethodSecurityConfig.class})
@Profile("test")
public class SecurityTestConfig {

  @Bean
  public ProtectedRepoFilePermissionEvaluator protectedRepoFilePermissionEvaluator() {
    HostNamePatternFilter accessFilter = new HostNamePatternFilter("protected-repo", "");
    return new ProtectedRepoFilePermissionEvaluator(accessFilter);
  }

  @Bean
  public ProviderManager providerManager() {
    return new ProviderManager(asList((AuthenticationProvider) new DevUsernameEqualsPasswordAuthenticationProvider()));
  }

  @Bean
  public GridFsService gridFsService() {
    MongoTemplate mongoTemplate = mock(MongoTemplate.class);
    DBCollection collection = mock(DBCollection.class);
    when(mongoTemplate.getCollection(anyString())).thenReturn(collection);
    GridFsTemplate gridFsTemplate = mock(GridFsTemplate.class);
    return new GridFsService(null, gridFsTemplate, mongoTemplate, null, null);
  }
}
