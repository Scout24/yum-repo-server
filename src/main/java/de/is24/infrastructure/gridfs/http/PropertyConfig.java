package de.is24.infrastructure.gridfs.http;

import com.google.common.collect.ImmutableSet;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.springframework.util.StringUtils.commaDelimitedListToSet;


public class PropertyConfig {
  private static final Set<String> PROPERTY_FILES = ImmutableSet.of("configuration.properties", "version.properties");
  private static final String CONFIGURATION_FILE_PROPERTY = "configurationFile";

  @Bean
  public PropertyPlaceholderConfigurer propertyPlaceholderConfigurer() {
    PropertyPlaceholderConfigurer propertyPlaceholderConfigurer = new PropertyPlaceholderConfigurer();
    propertyPlaceholderConfigurer.setSystemPropertiesMode(
      PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_OVERRIDE);
    propertyPlaceholderConfigurer.setSearchSystemEnvironment(true);
    propertyPlaceholderConfigurer.setLocations(getPropertiesFileResources());

    // Allow for other PropertyPlaceholderConfigurer instances.
    propertyPlaceholderConfigurer.setIgnoreUnresolvablePlaceholders(true);

    // allow to set null value as default in @Value("${any.property:@null}")
    propertyPlaceholderConfigurer.setNullValue("@null");
    return propertyPlaceholderConfigurer;
  }

  public Resource[] getPropertiesFileResources() {
    List<Resource> propertyResources = new ArrayList<>();

    for (String propertiesFile : PROPERTY_FILES) {
      Resource configurationResource = new ClassPathResource(propertiesFile);
      if (configurationResource.exists()) {
        propertyResources.add(configurationResource);
      }
    }

    if (System.getProperty(CONFIGURATION_FILE_PROPERTY) != null) {
      for (String propertyFilePath : commaDelimitedListToSet(System.getProperty(CONFIGURATION_FILE_PROPERTY))) {
        FileSystemResource configurationResource = new FileSystemResource(propertyFilePath);
        if (configurationResource.exists()) {
          propertyResources.add(configurationResource);
        }
      }
    }

    return propertyResources.toArray(new Resource[propertyResources.size()]);
  }
}
