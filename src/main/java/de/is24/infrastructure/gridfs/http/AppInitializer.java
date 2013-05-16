package de.is24.infrastructure.gridfs.http;

import de.is24.infrastructure.gridfs.http.log4j.MDCFilter;
import de.is24.infrastructure.gridfs.http.web.WebConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.DispatcherServlet;
import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import static de.is24.infrastructure.gridfs.http.Profiles.PROD;
import static java.lang.Integer.parseInt;
import static java.util.EnumSet.of;
import static javax.servlet.DispatcherType.FORWARD;
import static javax.servlet.DispatcherType.REQUEST;


public class AppInitializer implements WebApplicationInitializer {
  private static final Logger LOG = LoggerFactory.getLogger(AppInitializer.class);

  public static final String SERVLET_NAME = "appServlet";

  // the maximum size allowed for uploaded files = max file size for rpms
  public static final long MAX_FILE_SIZE = 2L * 1024 * 1024 * 1024;

  // the size threshold after which files will be written to disk (temporarily)
  public static final int FILE_SIZE_THRESHOLD_IN_MB = 5;
  public static final String UPLOAD_FILE_SIZE_THRESHOLD_KEY = "upload.file.size.threshold.in.mb";

  public void onStartup(ServletContext servletContext) throws ServletException {
    WebApplicationContext rootContext = createRootContext(servletContext);
    createSpringRootServlet(servletContext, rootContext);
    registerUrlRewirteFilter(servletContext);
    registerSecurityFilter(servletContext);
    registerContentTypeFilter(servletContext);
    registerMDCFilter(servletContext);
  }

  private AnnotationConfigWebApplicationContext createRootContext(ServletContext servletContext) {
    AnnotationConfigWebApplicationContext rootContext = new AnnotationConfigWebApplicationContext();
    rootContext.register(AppConfig.class, WebConfig.class);
    rootContext.getEnvironment().setDefaultProfiles(PROD);
    servletContext.addListener(new ContextLoaderListener(rootContext));
    return rootContext;
  }

  private void createSpringRootServlet(ServletContext servletContext, WebApplicationContext rootContext) {
    ServletRegistration.Dynamic appServlet = servletContext.addServlet(SERVLET_NAME,
      new DispatcherServlet(rootContext));
    appServlet.setLoadOnStartup(1);
    appServlet.addMapping("/");
    appServlet.setMultipartConfig(new MultipartConfigElement(null, MAX_FILE_SIZE, MAX_FILE_SIZE,
        fileSizeThresholdInMB() * 1024 * 1024));
  }

  private int fileSizeThresholdInMB() {
    if (System.getProperty(UPLOAD_FILE_SIZE_THRESHOLD_KEY) != null) {
      try {
        int value = parseInt(System.getProperty(UPLOAD_FILE_SIZE_THRESHOLD_KEY));
        LOG.info("Setting upload file size threshold to {}mb", value);
        return value;
      } catch (NumberFormatException e) {
        LOG.warn("Could not parse upload file size threshold and set default", e);
      }
    }
    return FILE_SIZE_THRESHOLD_IN_MB;
  }

  private void registerUrlRewirteFilter(ServletContext servletContext) {
    servletContext.addFilter("urlRewriteFilter", new UrlRewriteFilter())
    .addMappingForUrlPatterns(of(REQUEST, FORWARD), false, "/*");
  }

  private void registerSecurityFilter(ServletContext servletContext) {
    servletContext.addFilter("springSecurityFilterChain", new DelegatingFilterProxy())
    .addMappingForUrlPatterns(null, false, "/*");
  }

  private void registerContentTypeFilter(ServletContext servletContext) {
    servletContext.addFilter("formEncodedContentTypeFilter", new DelegatingFilterProxy())
    .addMappingForUrlPatterns(null, false, "/*");
  }

  private void registerMDCFilter(ServletContext servletContext) {
    servletContext.addFilter("mdcFilter", new MDCFilter()).addMappingForUrlPatterns(null, false, "/*");
  }

}
