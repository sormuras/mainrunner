package de.sormuras.mainrunner.engine;

import de.sormuras.mainrunner.api.Main;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

public class Mainrunner extends AbstractClassBasedTestEngine {

  private final Overlay overlay = OverlaySingleton.INSTANCE;

  @Override
  public String getId() {
    return "mainrunner";
  }

  @Override
  public String createEngineDisplayName() {
    return overlay.display();
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
  public TestDescriptor createTestMethod(TestDescriptor parent, Method method) {
    try {
      Class.forName("de.sormuras.mainrunner.api.Main");
    } catch (ClassNotFoundException e) {
      return newTestMethod(parent, method);
    }
    Main[] mains = method.getDeclaredAnnotationsByType(Main.class);
    if (mains.length == 0) {
      return newTestMethod(parent, method);
    }
    if (mains.length == 1) {
      return newTestMethod(parent, method, mains[0]);
    }
    throw new UnsupportedOperationException("Multiple @Main annotation aren't supported, yet!");
  }

  private TestDescriptor newTestMethod(TestDescriptor parent, Method method) {
    UniqueId testId = parent.getUniqueId().append("method", method.getName());
    String testDisplayName = method.getName() + "()";
    Object[] testArguments = new Object[] {new String[0]};
    return new TestMethod(testId, testDisplayName, method, testArguments);
  }

  private TestDescriptor newTestMethod(TestDescriptor parent, Method method, Main main) {
    UniqueId testId = parent.getUniqueId().append("method", method.getName() + main.hashCode());
    String name = main.displayName().replace("${ARGS}", String.join("\", \"", main.value()));
    Object[] args = new Object[] {main.value()};
    return new TestMethod(testId, name, method, args);
  }

  @Override
  public Object createTestInstance(Class<?> testClass) {
    return null; // all "main" methods are static, no instance needed
  }
}
