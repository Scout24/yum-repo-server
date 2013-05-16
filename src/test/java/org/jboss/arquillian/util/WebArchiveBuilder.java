package org.jboss.arquillian.util;

import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;

import static org.jboss.shrinkwrap.api.Filters.include;


public class WebArchiveBuilder {
  private PomEquippedResolveStage mavenDependencyResolver;
  private final WebArchive webArchive;

  public WebArchiveBuilder(String context) {
    mavenDependencyResolver = Maven.resolver().loadPomFromFile("pom.xml");
    webArchive = ShrinkWrap.create(WebArchive.class, context);
  }

  public WebArchiveBuilder withClassesToTest(Class<?>... classes) {
    webArchive.addClasses(classes);
    return this;
  }

  public WebArchiveBuilder withPackagesToTest(String... packageNames) {
    webArchive.addPackages(true, packageNames);
    return this;
  }

  public WebArchiveBuilder withSpringWebDependencies() {
    return withDependencies("org.springframework:spring-web",
      "org.springframework:spring-webmvc");
  }

  public WebArchiveBuilder withSpringSecurityDependencies() {
    webArchive.addAsLibraries(mavenDependencyResolver.resolve(
        "org.springframework.security:spring-security-core",
        "org.springframework.security:spring-security-web",
        "org.springframework.security:spring-security-config")
      .withoutTransitivity()
      .asFile());
    return this;
  }

  public WebArchiveBuilder withAllRuntimeDependencies() {
    webArchive.addAsLibraries(mavenDependencyResolver.importRuntimeDependencies().asFile());
    return this;
  }

  public WebArchiveBuilder withDependencies(String... ids) {
    webArchive.addAsLibraries(mavenDependencyResolver.resolve(ids).withTransitivity().asFile());
    return this;
  }

  public WebArchiveBuilder withResourceFiles(String... fileNames) {
    for (String fileName : fileNames) {
      webArchive.addAsResource(ClassLoader.getSystemResource(fileName), fileName);
    }
    return this;
  }

  public WebArchiveBuilder withSpringContexts(String... contextPaths) {
    for (String contextPath : contextPaths) {
      webArchive.addAsWebInfResource(contextPath);
    }
    return this;
  }

  public WebArchiveBuilder withAllResources() {
    webArchive.merge(ShrinkWrap.create(GenericArchive.class)
      .as(ExplodedImporter.class)
      .importDirectory("src/main/resources")
      .as(GenericArchive.class),
      "/WEB-INF/classes/", include(".*\\.(xml|properties)$"));
    return this;
  }


  public WebArchiveBuilder withAllJSPs() {
    webArchive.merge(ShrinkWrap.create(GenericArchive.class)
      .as(ExplodedImporter.class)
      .importDirectory("src/main/webapp")
      .as(GenericArchive.class),
      "/", include(".*\\.jsp$"));
    return this;
  }

  public WebArchiveBuilder withFacesSupport() {
    webArchive.addAsWebInfResource(
      new StringAsset("<faces-config version=\"2.0\"/>"),
      "faces-config.xml");
    return this;
  }

  public WebArchiveBuilder setWebXml(String webXmlPath) {
    webArchive.setWebXML(webXmlPath);
    return this;
  }

  public WebArchive build() {
    return webArchive;
  }


  public WebArchiveBuilder withTestResouce(String fileName) {
    String archivePath = "/WEB-INF/classes/" + fileName;
    if (webArchive.contains(archivePath)) {
      webArchive.delete(archivePath);
    }
    webArchive.addAsResource(ClassLoader.getSystemResource(fileName), fileName);
    return this;
  }
}
