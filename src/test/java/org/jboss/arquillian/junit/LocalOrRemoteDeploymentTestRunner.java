package org.jboss.arquillian.junit;

import org.jboss.arquillian.test.impl.enricher.resource.RemoteResourceTestEnricher;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isBlank;

public class LocalOrRemoteDeploymentTestRunner extends BlockJUnit4ClassRunner {

  public static final String PROPERTY_KEY = "remoteContainerUrl";
  private final Arquillian arquillianDelegator;

  public LocalOrRemoteDeploymentTestRunner(Class<?> klass) throws InitializationError {
    super(klass);

    if (isBlank(System.getProperty(PROPERTY_KEY))) {
      arquillianDelegator = new Arquillian(klass);
    } else {
      arquillianDelegator = null;
    }
  }

  @Override
  protected List<FrameworkMethod> getChildren() {
    if (arquillianActive()) {
      return arquillianDelegator.getChildren();
    }

    List<FrameworkMethod> filteredTestMethods = new ArrayList<FrameworkMethod>();
    for (FrameworkMethod testMethod : super.getChildren()) {
      if (testMethod.getAnnotation(LocalOnly.class) == null) {
        filteredTestMethods.add(testMethod);
      }
    }

    return filteredTestMethods;
  }

  private boolean arquillianActive() {
    return arquillianDelegator != null;
  }

  @Override
  public void run(RunNotifier notifier) {
    if (arquillianActive()) {
      arquillianDelegator.run(notifier);
    } else {
      super.run(notifier);
    }
  }

  @Override
  protected Statement withAfterClasses(Statement statement) {
    if (arquillianActive()) {
      return arquillianDelegator.withAfterClasses(statement);
    }

    return super.withAfterClasses(statement);
  }

  @Override
  protected Statement withBeforeClasses(Statement statement) {
    if (arquillianActive()) {
      return arquillianDelegator.withBeforeClasses(statement);
    }
    return super.withBeforeClasses(statement);
  }

  @Override
  protected void validatePublicVoidNoArgMethods(Class<? extends Annotation> annotation, boolean isStatic, List<Throwable> errors) {
    if (arquillianActive()) {
      arquillianDelegator.validatePublicVoidNoArgMethods(annotation, isStatic, errors);
    } else {
      super.validatePublicVoidNoArgMethods(annotation, isStatic, errors);
    }
  }

  @Override
  protected Statement withAfters(FrameworkMethod method, Object target, Statement statement) {
    if (arquillianActive()) {
      return arquillianDelegator.withAfters(method, target, statement);
    }

    return super.withAfters(method, target, statement);
  }

  @Override
  protected Statement withBefores(FrameworkMethod method, Object target, Statement statement) {
    if (arquillianActive()) {
      return arquillianDelegator.withBefores(method, target, statement);
    }

    try {
      new RemoteResourceTestEnricher(new URL(System.getProperty(PROPERTY_KEY))).enrich(target);
    } catch (MalformedURLException e) {
      throw new RuntimeException("Could not parse remote URL.", e);
    }
    return super.withBefores(method, target, statement);
  }


}
