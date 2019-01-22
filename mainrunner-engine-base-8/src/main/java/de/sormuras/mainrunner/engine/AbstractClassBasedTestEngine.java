package de.sormuras.mainrunner.engine;

import java.lang.reflect.Method;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;

public abstract class AbstractClassBasedTestEngine implements TestEngine {

  @Override
  public TestDescriptor discover(EngineDiscoveryRequest engineDiscoveryRequest, UniqueId uniqueId) {
    // create descriptor tree using `isTestClass` and `isTestMethod` predicates...
    return null;
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
