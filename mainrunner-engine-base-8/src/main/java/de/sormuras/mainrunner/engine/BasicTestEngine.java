package de.sormuras.mainrunner.engine;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.junit.platform.engine.TestExecutionResult;

public class BasicTestEngine extends AbstractClassBasedTestEngine {

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
  public TestExecutionResult executeMethod(Method method) {
    try {
      method.invoke(null);
    } catch (ReflectiveOperationException e) {
      return TestExecutionResult.failed(e);
    }
    return TestExecutionResult.successful();
  }

  @Override
  public String getId() {
    return "basic";
  }
}
