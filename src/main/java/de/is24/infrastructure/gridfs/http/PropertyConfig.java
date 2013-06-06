package de.is24.infrastructure.gridfs.http;

import com.google.common.collect.ImmutableSet;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import java.util.ArrayList;
import java.util.Set;


public class PropertyConfig {
  private static final Set<String> PROPERTY_FILES = ImmutableSet.of("configuration.properties", "version.properties");

  @Bean
  public PropertyPlaceholderConfigurer propertyPlaceholderConfigurer() {
    PropertyPlaceholderConfigurer propertyPlaceholderConfigurer = new PropertyPlaceholderConfigurer();
    propertyPlaceholderConfigurer.setSystemPropertiesMode(
      PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_OVERRIDE);
    propertyPlaceholderConfigurer.setSearchSystemEnvironment(true);

    ArrayList<Resource> propertyResources = new ArrayList<>();
    // if configuration properties exists

    for (String propertiesFile : PROPERTY_FILES) {
      ClassPathResource configurationResource = new ClassPathResource(propertiesFile);
      if (configurationResource.exists()) {
        propertyResources.add(configurationResource);
      }
    }
    propertyPlaceholderConfigurer.setLocations(propertyResources.toArray(new Resource[] {}));

    // Allow for other PropertyPlaceholderConfigurer instances.
    propertyPlaceholderConfigurer.setIgnoreUnresolvablePlaceholders(true);

    // allow to set null value as default in @Value("${any.property:@null}")
    propertyPlaceholderConfigurer.setNullValue("@null");
    return propertyPlaceholderConfigurer;
  }
}
