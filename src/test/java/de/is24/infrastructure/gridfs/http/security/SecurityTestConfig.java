package de.is24.infrastructure.gridfs.http.security;

import com.mongodb.DBCollection;
import de.is24.infrastructure.gridfs.http.gridfs.GridFsFileStorageService;
import de.is24.infrastructure.gridfs.http.gridfs.StorageService;
import de.is24.infrastructure.gridfs.http.storage.FileStorageService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
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
    ProtectedRepoAccessEvaluator accaccessEvaluator = new ProtectedRepoAccessEvaluator("protected-repo", "");
    return new ProtectedRepoFilePermissionEvaluator(accaccessEvaluator);
  }

  @Bean
  public ProviderManager providerManager() {
    return new ProviderManager(asList((AuthenticationProvider) new DevUsernameEqualsPasswordAuthenticationProvider()));
  }

  @Bean
  public StorageService gridFsService() {
    FileStorageService fileStorageService = mock(FileStorageService.class);
    return new StorageService(fileStorageService, null, null);
  }

  @Bean
  public GridFsFileStorageService gridFsFileStorageService() {
    MongoTemplate mongoTemplate = mock(MongoTemplate.class);
    DBCollection mockCollection = mock(DBCollection.class);
    when(mongoTemplate.getCollection(anyString())).thenReturn(mockCollection);
    return new GridFsFileStorageService(null, null, mongoTemplate);
  }
}
