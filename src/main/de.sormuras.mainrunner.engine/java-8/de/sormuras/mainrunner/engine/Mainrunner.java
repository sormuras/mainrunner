package de.sormuras.mainrunner.engine;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

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
  public String createTestMethodDisplayName(Method method) {
    return method.getName() + "()";
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
  public Object createTestInstance(Class<?> testClass) {
    return null; // all test methods are static, no instance needed
  }

  @Override
  public Object[] createTestArguments(Method method) {
    return new Object[] {new String[0]}; // pass an empty String[] to each main method
  }
}
