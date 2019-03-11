package de.sormuras.mainrunner.engine;

import static org.junit.platform.commons.util.ReflectionUtils.findAllClassesInClasspathRoot;
import static org.junit.platform.engine.support.filter.ClasspathScanningSupport.buildClassNamePredicate;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import org.junit.platform.commons.util.ClassFilter;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClasspathRootSelector;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.engine.support.descriptor.MethodSource;

public abstract class AbstractClassBasedTestEngine implements TestEngine {

  private static class TestClass extends AbstractTestDescriptor {

    private final Class<?> testClass;

    private TestClass(UniqueId uniqueId, String displayName, Class<?> testClass) {
      super(uniqueId, displayName, ClassSource.from(testClass));
      this.testClass = testClass;
    }

    Class<?> getTestClass() {
      return testClass;
    }

    @Override
    public Type getType() {
      return Type.CONTAINER;
    }
  }

  private static class TestMethod extends AbstractTestDescriptor {
    private final Method testMethod;
    private final Object[] arguments;

    private TestMethod(
        UniqueId uniqueId, String displayName, Method testMethod, Object[] arguments) {
      super(uniqueId, displayName, MethodSource.from(testMethod));
      this.testMethod = testMethod;
      this.arguments = arguments;
    }

    Object[] getArguments() {
      return arguments;
    }

    Method getTestMethod() {
      return testMethod;
    }

    @Override
    public Type getType() {
      return Type.TEST;
    }
  }

  @Override
  public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
    EngineDescriptor engine = new EngineDescriptor(uniqueId, getClass().getSimpleName());
    ClassFilter classFilter = ClassFilter.of(buildClassNamePredicate(discoveryRequest), c -> true);

    // class-path root
    discoveryRequest.getSelectorsByType(ClasspathRootSelector.class).stream()
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
    UniqueId classId = engine.getUniqueId().append("class", candidate.getName());
    String classDisplayName = createTestClassDisplayName(candidate);
    TestClass testClass = new TestClass(classId, classDisplayName, candidate);
    engine.addChild(testClass);
    for (Method method : candidate.getMethods()) {
      if (!isTestMethod(method)) {
        continue;
      }
      UniqueId testId = testClass.getUniqueId().append("method", method.getName());
      String testDisplayName = getTestMethodDisplayName(method);
      Object[] testArguments = getTestArguments(method);
      TestMethod testMethod = new TestMethod(testId, testDisplayName, method, testArguments);
      testClass.addChild(testMethod);
    }
  }

  public abstract boolean isTestClass(Class<?> candidate);

  public abstract boolean isTestMethod(Method method);

  public String createTestClassDisplayName(Class<?> testClass) {
    return testClass.getSimpleName();
  }

  public String getTestMethodDisplayName(Method method) {
    return method.getName() + "(" + Arrays.asList(method.getParameterTypes()) + ")";
  }

  public Object[] getTestArguments(Method method) {
    if (method.getParameterCount() == 0) {
      return new Object[0];
    }
    throw new RuntimeException("Overriding getTestArguments(Method) might help");
  }

  public Object createTestInstance(Class<?> testClass) {
    try {
      return testClass.getConstructor().newInstance();
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException("Overriding createTestInstance(Class) might help", e);
    }
  }

  @Override
  public void execute(ExecutionRequest request) {
    TestDescriptor engine = request.getRootTestDescriptor();
    EngineExecutionListener listener = request.getEngineExecutionListener();
    listener.executionStarted(engine);
    for (TestDescriptor child : engine.getChildren()) {
      listener.executionStarted(child);
      if (child instanceof TestClass) {
        execute(listener, (TestClass) child);
      }
      listener.executionFinished(child, TestExecutionResult.successful());
    }
    listener.executionFinished(engine, TestExecutionResult.successful());
  }

  public void execute(EngineExecutionListener listener, TestClass testClass) {
    Object testInstance = createTestInstance(testClass.getTestClass());
    for (TestDescriptor mainMethod : testClass.getChildren()) {
      listener.executionStarted(mainMethod);
      TestExecutionResult result = execute(testInstance, (TestMethod) mainMethod);
      listener.executionFinished(mainMethod, result);
    }
  }

  public TestExecutionResult execute(Object testInstance, TestMethod testMethod) {
    try {
      Method method = testMethod.getTestMethod();
      Object[] arguments = testMethod.getArguments();
      method.invoke(testInstance, arguments);
    } catch (ReflectiveOperationException e) {
      return TestExecutionResult.failed(e);
    }
    return TestExecutionResult.successful();
  }
}
