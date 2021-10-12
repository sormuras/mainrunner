package com.github.sormuras.mainrunner.engine;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
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

  private class Resolver implements SelectorResolver {

    private final Predicate<String> classNameFilter;

    Resolver(Predicate<String> classNameFilter) {
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
              .addToParent(parent -> Optional.of(createTestClassDescriptor(parent, candidate)))
              .orElseThrow(Error::new);
      return Resolution.match(Match.exact(descriptor, () -> createMethodSelectors(candidate)));
    }

    private TestDescriptor createTestClassDescriptor(TestDescriptor parent, Class<?> candidate) {
      UniqueId classId = parent.getUniqueId().append("class", candidate.getName());
      String classDisplayName = createTestClassDisplayName(candidate);
      return new TestClass(classId, classDisplayName, candidate);
    }

    private Set<MethodSelector> createMethodSelectors(Class<?> candidate) {
      List<Method> methods =
          ReflectionSupport.findMethods(
              candidate,
              AbstractClassBasedTestEngine.this::isTestMethod,
              HierarchyTraversalMode.TOP_DOWN);
      return methods.stream().map(method -> selectMethod(candidate, method)).collect(toSet());
    }

    @Override
    public Resolution resolve(MethodSelector selector, SelectorResolver.Context context) {
      Method method = selector.getJavaMethod();
      if (!isTestMethod(method)) {
        return Resolution.unresolved();
      }
      Set<Match> matches =
          createTestMethods(method)
              .map(
                  descriptorCreator ->
                      context
                          .addToParent(
                              () -> selectClass(selector.getJavaClass()),
                              parent -> Optional.of(descriptorCreator.apply(parent)))
                          .orElseThrow(Error::new))
              .map(Match::exact)
              .collect(toSet());
      return Resolution.matches(matches);
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

  protected static class TestMethod extends AbstractTestDescriptor {
    private final Method testMethod;
    private final Object[] arguments;

    protected TestMethod(
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
    EngineDescriptor engine = new EngineDescriptor(uniqueId, createEngineDisplayName());
    EngineDiscoveryRequestResolver<EngineDescriptor> resolver =
        EngineDiscoveryRequestResolver.<EngineDescriptor>builder()
            .addClassContainerSelectorResolver(this::isTestClass)
            .addSelectorResolver(context -> new Resolver(context.getClassNameFilter()))
            .addTestDescriptorVisitor(context -> TestDescriptor::prune)
            .build();
    resolver.resolve(discoveryRequest, engine);
    return engine;
  }

  public abstract boolean isTestClass(Class<?> candidate);

  public abstract boolean isTestMethod(Method method);

  public String createEngineDisplayName() {
    return getClass().getSimpleName();
  }

  public String createTestClassDisplayName(Class<?> testClass) {
    return testClass.getSimpleName();
  }

  public String createTestMethodDisplayName(Method method) {
    return method.getName() + "(" + Arrays.asList(method.getParameterTypes()) + ")";
  }

  public Object[] createTestArguments(Method method) {
    if (method.getParameterCount() == 0) {
      return new Object[0];
    }
    // Create an empty array of type X argument...
    Class<?> type0 = method.getParameterTypes()[0];
    if (method.getParameterCount() == 1 && type0.isArray()) {
      return new Object[] {Array.newInstance(type0.getComponentType(), 0)};
    }
    throw new RuntimeException("Overriding createTestArguments(Method) might help");
  }

  public Stream<UnaryOperator<TestDescriptor>> createTestMethods(Method method) {
    return Stream.of(
        parent -> {
          UniqueId testId = parent.getUniqueId().append("method", method.getName());
          String testDisplayName = createTestMethodDisplayName(method);
          Object[] testArguments = createTestArguments(method);
          return new TestMethod(testId, testDisplayName, method, testArguments);
        });
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
      Throwable cause = e.getCause();
      if (cause != null) {
        return TestExecutionResult.failed(cause);
      }
      return TestExecutionResult.failed(e);
    }
    return TestExecutionResult.successful();
  }
}
