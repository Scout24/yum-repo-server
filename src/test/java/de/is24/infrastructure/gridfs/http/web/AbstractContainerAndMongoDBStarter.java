package de.is24.infrastructure.gridfs.http.web;

import com.mongodb.Mongo;
import de.is24.infrastructure.gridfs.http.SpringBootAppConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.net.URL;

import static de.is24.infrastructure.gridfs.http.Profiles.DEV;
import static de.is24.infrastructure.gridfs.http.utils.RepositoryUtils.getHttpClientBuilder;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SpringBootAppConfig.class, initializers = DeploymentUrlApplicationContextInitializer.class)
@WebAppConfiguration
@IntegrationTest("server.port=0")
@ActiveProfiles(DEV)
public class AbstractContainerAndMongoDBStarter {

  @Value("${local.deployment.url}")
  public URL deploymentURL;

  @Value("${local.server.port}")
  protected int port;

  @Autowired
  protected SpringBootAppConfig appConfig;

  @Autowired
  protected Mongo mongo;

  protected CloseableHttpClient httpClient;

  @Before
  public void setDeploymentUrl() {
    httpClient = getHttpClientBuilder().build();
  }

  public void startMongo() throws Throwable {
    appConfig.startMongo();
  }

  public void stopMongo() {
    appConfig.stopMongo();
  }

}
