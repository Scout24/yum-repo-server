package de.is24.infrastructure.gridfs.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import com.google.common.collect.ImmutableSet;


public class PropertyConfig {
  private static final Logger LOGGER = LoggerFactory.getLogger(PropertyConfig.class);
  private static final Set<String> PROPERTY_FILES = ImmutableSet.of("configuration.properties", "version.properties");

  @Bean
  public PropertyPlaceholderConfigurer propertyPlaceholderConfigurer() {
    PropertyPlaceholderConfigurer propertyPlaceholderConfigurer = new PropertyPlaceholderConfigurer();
    propertyPlaceholderConfigurer.setSystemPropertiesMode(
      PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_OVERRIDE);
    propertyPlaceholderConfigurer.setSearchSystemEnvironment(true);

    List<Resource> propertyResources = new ArrayList<>();
    // if configuration properties exists

    for (String propertiesFile : PROPERTY_FILES) {
      Resource configurationResource = null;
      if (System.getProperty(propertiesFile+"File") == null) {
        configurationResource = new ClassPathResource(propertiesFile);
      } else {
        configurationResource = new FileSystemResource(System.getProperty(propertiesFile+"File"));
        LOGGER.info("file based configuration resource defined with {} ",System.getProperty(propertiesFile+"File"));
      }
      if (configurationResource.exists()) {
        propertyResources.add(configurationResource);
      } else {
          LOGGER.error("error resource {} doesn't exist",configurationResource.toString());
      }
    }
    propertyPlaceholderConfigurer.setLocations(propertyResources.toArray(new Resource[propertyResources.size()]));

    // Allow for other PropertyPlaceholderConfigurer instances.
    propertyPlaceholderConfigurer.setIgnoreUnresolvablePlaceholders(true);

    // allow to set null value as default in @Value("${any.property:@null}")
    propertyPlaceholderConfigurer.setNullValue("@null");
    return propertyPlaceholderConfigurer;
  }
}
