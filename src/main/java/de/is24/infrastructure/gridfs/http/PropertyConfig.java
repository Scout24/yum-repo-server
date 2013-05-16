package de.is24.infrastructure.gridfs.http;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;


public class PropertyConfig {
  @Bean
  public PropertyPlaceholderConfigurer propertyPlaceholderConfigurer() {
    PropertyPlaceholderConfigurer propertyPlaceholderConfigurer = new PropertyPlaceholderConfigurer();
    propertyPlaceholderConfigurer.setSystemPropertiesMode(
      PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_OVERRIDE);
    propertyPlaceholderConfigurer.setSearchSystemEnvironment(true);

    // if configuration properties exists
    ClassPathResource configurationResource = new ClassPathResource("configuration.properties");
    if (configurationResource.exists()) {
      propertyPlaceholderConfigurer.setLocation(configurationResource);
    }

    // Allow for other PropertyPlaceholderConfigurer instances.
    propertyPlaceholderConfigurer.setIgnoreUnresolvablePlaceholders(true);

    // allow to set null value as default in @Value("${any.property:@null}")
    propertyPlaceholderConfigurer.setNullValue("@null");
    return propertyPlaceholderConfigurer;
  }
}
