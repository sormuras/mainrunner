package de.sormuras.mainrunner.engine;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.engine.support.discovery.EngineDiscoveryRequestResolver;
import org.junit.platform.engine.support.discovery.SelectorResolver;

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
    EngineDiscoveryRequestResolver<EngineDescriptor> resolver =
        EngineDiscoveryRequestResolver.<EngineDescriptor>builder()
            .addClassContainerSelectorResolver(this::isTestClass)
            .addSelectorResolver(
                context -> new ClassBasedSelectorResolver(context.getClassNameFilter()))
            .addTestDescriptorVisitor(context -> TestDescriptor::prune)
            .build();
    resolver.resolve(discoveryRequest, engine);
    return engine;
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

  class ClassBasedSelectorResolver implements SelectorResolver {

    private final Predicate<String> classNameFilter;

    ClassBasedSelectorResolver(Predicate<String> classNameFilter) {
      this.classNameFilter = classNameFilter;
    }

    @Override
    public Resolution resolve(ClassSelector selector, SelectorResolver.Context context) {
      if (!classNameFilter.test(selector.getClassName()) || !isTestClass(selector.getJavaClass())) {
        return Resolution.unresolved();
      }
      Class<?> candidate = selector.getJavaClass();
      TestDescriptor descriptor =
          context
              .addToParent(parent -> Optional.of(resolve(parent, candidate)))
              .orElseThrow(Error::new);
      return Resolution.match(
          Match.exact(
              descriptor,
              () ->
                  ReflectionSupport.findMethods(
                          candidate,
                          AbstractClassBasedTestEngine.this::isTestMethod,
                          HierarchyTraversalMode.TOP_DOWN)
                      .stream()
                      .map(method -> selectMethod(candidate, method))
                      .collect(toSet())));
    }

    private TestDescriptor resolve(TestDescriptor parent, Class<?> candidate) {
      UniqueId classId = parent.getUniqueId().append("class", candidate.getName());
      String classDisplayName = createTestClassDisplayName(candidate);
      return new AbstractClassBasedTestEngine.TestClass(classId, classDisplayName, candidate);
    }

    @Override
    public Resolution resolve(MethodSelector selector, SelectorResolver.Context context) {
      Method method = selector.getJavaMethod();
      if (!isTestMethod(method)) {
        return Resolution.unresolved();
      }
      TestDescriptor descriptor =
          context
              .addToParent(
                  () -> selectClass(selector.getJavaClass()),
                  parent -> Optional.of(resolve(parent, method)))
              .orElseThrow(Error::new);
      return Resolution.match(Match.exact(descriptor));
    }

    private TestDescriptor resolve(TestDescriptor parent, Method method) {
      UniqueId testId = parent.getUniqueId().append("method", method.getName());
      String testDisplayName = getTestMethodDisplayName(method);
      Object[] testArguments = getTestArguments(method);
      return new AbstractClassBasedTestEngine.TestMethod(
          testId, testDisplayName, method, testArguments);
    }

    @Override
    public Resolution resolve(UniqueIdSelector selector, SelectorResolver.Context context) {
      UniqueId.Segment lastSegment = selector.getUniqueId().getLastSegment();
      if (lastSegment.getType().equals("class")) {
        return Resolution.selectors(singleton(selectClass(lastSegment.getValue())));
      }
      if (lastSegment.getType().equals("method")) {
        UniqueId uniqueIdOfClass = selector.getUniqueId().removeLastSegment();
        String className = uniqueIdOfClass.getLastSegment().getValue();
        return Resolution.selectors(singleton(selectMethod(className, lastSegment.getValue())));
      }
      return Resolution.unresolved();
    }
  }
}
