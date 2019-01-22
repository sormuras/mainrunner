package de.sormuras.mainrunner.engine;

import static org.junit.platform.commons.util.ReflectionUtils.findAllClassesInClasspathRoot;
import static org.junit.platform.engine.support.filter.ClasspathScanningSupport.buildClassNamePredicate;

import java.lang.reflect.Method;
import java.util.Collection;
import org.junit.platform.commons.util.ClassFilter;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClasspathRootSelector;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

public abstract class AbstractClassBasedTestEngine implements TestEngine {

  @Override
  public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
    EngineDescriptor engine = new EngineDescriptor(uniqueId, getClass().getSimpleName());
    ClassFilter classFilter = ClassFilter.of(buildClassNamePredicate(discoveryRequest), c -> true);

    // class-path root
    discoveryRequest
        .getSelectorsByType(ClasspathRootSelector.class)
        .stream()
        .map(ClasspathRootSelector::getClasspathRoot)
        .map(uri -> findAllClassesInClasspathRoot(uri, classFilter))
        .flatMap(Collection::stream)
        .forEach(candidate -> handleCandidate(engine, candidate));

    return engine;
  }

  private void handleCandidate(EngineDescriptor engine, Class<?> candidate) {
    if (!isTestClass(candidate)) {
      return;
    }
    MainClass container = MainClass.of(candidate, engine);
    for (Method method : candidate.getMethods()) {
      if (!isTestMethod(method)) {
        continue;
      }
      UniqueId id = container.getUniqueId().append("main", "main0");
      container.addChild(new MainMethod(id, method));
    }
  }

  public abstract boolean isTestClass(Class<?> candidate);

  public abstract boolean isTestMethod(Method method);

  public String getContainerIdFragment() {
    return getId() + "-class";
  }

  public String getContainerDisplayName(Class<?> container) {
    return container.getSimpleName();
  }

  public String getTestIdFragment() {
    return getId() + "-test";
  }

  public String getTestDisplayName(Method method) {
    return method.getName() + "()";
  }

  @Override
  public void execute(ExecutionRequest executionRequest) {
    // parse descriptor tree...
    // ...and dispatch to `executeMethod()`
    // ...report result to listeners
  }

  public abstract TestExecutionResult executeMethod(Method method);
}
