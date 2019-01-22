package de.sormuras.mainrunner.engine;

import static org.junit.platform.commons.util.ReflectionUtils.isPublic;
import static org.junit.platform.commons.util.ReflectionUtils.isStatic;
import static org.junit.platform.commons.util.ReflectionUtils.returnsVoid;

import java.lang.reflect.Method;

public class BasicTestEngine extends AbstractClassBasedTestEngine {

  @Override
  public boolean isContainer(Class<?> candidate) {
    try {
      Method main = candidate.getDeclaredMethod("main", String[].class);
      return isTest(main);
    } catch (NoSuchMethodException e) {
      return false;
    }
  }

  @Override
  public boolean isTest(Method main) {
    return isPublic(main) && isStatic(main) && returnsVoid(main);
  }

  @Override
  public void executeMethod(Method method) throws ReflectiveOperationException {
    method.invoke(null);
  }

  @Override
  public String getId() {
    return "basic-main-engine";
  }
}
