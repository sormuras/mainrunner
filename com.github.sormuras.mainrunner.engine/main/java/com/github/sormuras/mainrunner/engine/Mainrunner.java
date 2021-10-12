package com.github.sormuras.mainrunner.engine;

import com.github.sormuras.mainrunner.api.Run;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

public class Mainrunner extends AbstractClassBasedTestEngine {

  @Override
  public String getId() {
    return "mainrunner";
  }

  @Override
  public String createEngineDisplayName() {
    var module = getClass().getModule();
    var descriptor = module.getDescriptor();
    return "Mainrunner (" + (descriptor != null ? descriptor.toNameAndVersion() : module) + ")";
  }

  @Override
  public String createTestClassDisplayName(Class<?> testClass) {
    var name = testClass.getCanonicalName();
    var module = testClass.getModule();
    return module.isNamed() ? module.getName() + "/" + name : name;
  }

  @Override
  public String createTestMethodDisplayName(Method method) {
    return "main()";
  }

  @Override
  public boolean isTestClass(Class<?> candidate) {
    try {
      Method main = candidate.getDeclaredMethod("main", String[].class);
      return isTestMethod(main);
    } catch (NoSuchMethodException e) {
      return false;
    }
  }

  @Override
  public boolean isTestMethod(Method method) {
    Class<?>[] parameterTypes = {String[].class};
    int modifiers = method.getModifiers();
    return Modifier.isPublic(modifiers)
        && Modifier.isStatic(modifiers)
        && method.getReturnType().equals(void.class)
        && method.getName().equals("main")
        && Arrays.equals(method.getParameterTypes(), parameterTypes);
  }

  @Override
  public Stream<UnaryOperator<TestDescriptor>> createTestMethods(Method method) {
    var layer = Mainrunner.class.getModule().getLayer();
    if (layer.findModule("com.github.sormuras.mainrunner.api").isEmpty()) {
      return super.createTestMethods(method);
    }
    Run[] mains = method.getDeclaredAnnotationsByType(Run.class);
    if (mains.length == 0) {
      return super.createTestMethods(method);
    }
    return Arrays.stream(mains).map(main -> (parent -> newTestMethod(parent, method, main)));
  }

  private TestDescriptor newTestMethod(TestDescriptor parent, Method method, Run main) {
    UniqueId testId = parent.getUniqueId().append("method", method.getName() + main.hashCode());
    Object[] args = new Object[] {main.value()};
    return new TestMethod(testId, displayName(main), method, args);
  }

  @Override
  public Object createTestInstance(Class<?> testClass) {
    return null; // all "main" methods are static, no instance needed
  }

  private static String displayName(Run main) {
    String displayName = main.displayName();
    String args = main.value().length > 0 ? '"' + String.join("\", \"", main.value()) + '"' : "";
    if (displayName.length() > 0) {
      return displayName.replace("${ARGS}", args);
    }
    return "main(" + args + ")";
  }
}
