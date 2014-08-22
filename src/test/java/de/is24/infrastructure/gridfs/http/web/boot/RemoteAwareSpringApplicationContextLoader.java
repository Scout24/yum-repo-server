package de.is24.infrastructure.gridfs.http.web.boot;

import org.springframework.boot.test.SpringApplicationContextLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.web.WebMergedContextConfiguration;

import java.util.ArrayList;

import static de.is24.infrastructure.gridfs.http.Profiles.LOCAL_TESTS;
import static de.is24.infrastructure.gridfs.http.Profiles.REMOTE_TESTS;
import static de.is24.infrastructure.gridfs.http.web.boot.LocalConfig.isRemoteEnabled;
import static java.util.Arrays.asList;
import static org.springframework.test.util.ReflectionTestUtils.getField;

public class RemoteAwareSpringApplicationContextLoader extends SpringApplicationContextLoader  {

  @Override
  public ApplicationContext loadContext(MergedContextConfiguration config) throws Exception {
    ArrayList<String> activeProfiles = new ArrayList<>(asList(config.getActiveProfiles()));
    activeProfiles.add(isRemoteEnabled() ? REMOTE_TESTS : LOCAL_TESTS);
    WebMergedContextConfiguration newConfig = new WebMergedContextConfiguration(
        config.getTestClass(),
        config.getLocations(),
        config.getClasses(),
        config.getContextInitializerClasses(),
        activeProfiles.toArray(new String[activeProfiles.size()]),
        ((WebMergedContextConfiguration) config).getResourceBasePath(),
        config.getContextLoader(),
        (CacheAwareContextLoaderDelegate) getField(config, "cacheAwareContextLoaderDelegate"),
        config.getParent()
    );
    return super.loadContext(newConfig);
  }
}
