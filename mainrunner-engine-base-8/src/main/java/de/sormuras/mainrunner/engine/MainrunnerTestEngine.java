/*
 * Copyright (C) 2018 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.sormuras.mainrunner.engine;

import static java.lang.System.identityHashCode;
import static org.junit.platform.commons.util.ReflectionUtils.findAllClassesInClasspathRoot;
import static org.junit.platform.commons.util.ReflectionUtils.findAllClassesInPackage;
import static org.junit.platform.commons.util.ReflectionUtils.isPublic;
import static org.junit.platform.commons.util.ReflectionUtils.isStatic;
import static org.junit.platform.commons.util.ReflectionUtils.returnsVoid;
import static org.junit.platform.engine.support.filter.ClasspathScanningSupport.buildClassNamePredicate;

import de.sormuras.mainrunner.api.Main;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.platform.commons.util.ClassFilter;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.ClasspathRootSelector;
import org.junit.platform.engine.discovery.PackageSelector;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

/** Main-invoking TestEngine implementation. */
public class MainrunnerTestEngine implements TestEngine {

  @Override
  public String getId() {
    return "mainrunner";
  }

  @Override
  public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
    EngineDescriptor engine = new EngineDescriptor(uniqueId, OverlaySingleton.INSTANCE.display());

    ClassFilter classFilter = ClassFilter.of(buildClassNamePredicate(discoveryRequest), c -> true);

    // class-path root
    discoveryRequest
        .getSelectorsByType(ClasspathRootSelector.class)
        .stream()
        .map(ClasspathRootSelector::getClasspathRoot)
        .map(uri -> findAllClassesInClasspathRoot(uri, classFilter))
        .flatMap(Collection::stream)
        .forEach(candidate -> handleCandidate(engine, candidate));

    // package
    discoveryRequest
        .getSelectorsByType(PackageSelector.class)
        .stream()
        .map(PackageSelector::getPackageName)
        .map(packageName -> findAllClassesInPackage(packageName, classFilter))
        .flatMap(Collection::stream)
        .forEach(candidate -> handleCandidate(engine, candidate));

    // class
    discoveryRequest
        .getSelectorsByType(ClassSelector.class)
        .stream()
        .map(ClassSelector::getJavaClass)
        .filter(classFilter)
        .forEach(candidate -> handleCandidate(engine, candidate));

    return engine;
  }

  private void handleCandidate(EngineDescriptor engine, Class<?> candidate) {
    Method main;
    try {
      main = candidate.getDeclaredMethod("main", String[].class);
    } catch (NoSuchMethodException e) {
      return;
    }
    if (!isPublic(main)) {
      return;
    }
    if (!isStatic(main)) {
      return;
    }
    if (!returnsVoid(main)) {
      return;
    }
    MainClass container = MainClass.of(candidate, engine);
    Main[] annotations = main.getDeclaredAnnotationsByType(Main.class);
    if (annotations.length == 0) {
      UniqueId id = container.getUniqueId().append("main", "main0");
      container.addChild(new MainMethod(id, main));
      return;
    }
    for (Main annotation : annotations) {
      String value = "main" + identityHashCode(annotation);
      UniqueId id = container.getUniqueId().append("main", value);
      container.addChild(new MainMethod(id, main, annotation));
    }
  }

  @Override
  public void execute(ExecutionRequest request) {
    TestDescriptor engine = request.getRootTestDescriptor();
    EngineExecutionListener listener = request.getEngineExecutionListener();
    listener.executionStarted(engine);
    for (TestDescriptor mainClass : engine.getChildren()) {
      listener.executionStarted(mainClass);
      for (TestDescriptor mainMethod : mainClass.getChildren()) {
        listener.executionStarted(mainMethod);
        TestExecutionResult result = executeMainMethod(((MainMethod) mainMethod));
        listener.executionFinished(mainMethod, result);
      }
      listener.executionFinished(mainClass, TestExecutionResult.successful());
    }
    listener.executionFinished(engine, TestExecutionResult.successful());
  }

  private TestExecutionResult executeMainMethod(MainMethod mainMethod) {
    return mainMethod.isFork() ? executeForked(mainMethod) : executeDirect(mainMethod);
  }

  private TestExecutionResult executeDirect(MainMethod mainMethod) {
    try {
      Method method = mainMethod.getMethod();
      Object[] arguments = new Object[] {mainMethod.getArguments()};
      method.invoke(null, arguments);
    } catch (Throwable t) {
      return TestExecutionResult.failed(t);
    }
    return TestExecutionResult.successful();
  }

  private TestExecutionResult executeForked(MainMethod mainMethod) {
    String java = OverlaySingleton.INSTANCE.java().normalize().toAbsolutePath().toString();
    ProcessBuilder builder = new ProcessBuilder(java);
    List<String> command = builder.command();
    Arrays.stream(mainMethod.getOptions())
        .map(MainrunnerTestEngine::replaceSystemProperties)
        .forEach(command::add);
    command.add(mainMethod.getMethod().getDeclaringClass().getName());
    command.addAll(Arrays.asList(mainMethod.getArguments()));
    builder.inheritIO();
    try {
      Process process = builder.start();
      int actual = process.waitFor();
      int expected = mainMethod.getExpectedExitValue();
      if (actual != expected) {
        String message = "expected exit value " + expected + ", but got: " + actual;
        return TestExecutionResult.failed(new IllegalStateException(message));
      }
    } catch (IOException | InterruptedException e) {
      return TestExecutionResult.failed(e);
    }
    return TestExecutionResult.successful();
  }

  // https://docs.oracle.com/javase/8/docs/api/java/lang/System.html#getProperties--
  private static String replaceSystemProperties(String string) {
    string = replaceSystemProperty(string, "java.version"); // Java version number
    string = replaceSystemProperty(string, "java.version.date"); // Java version date
    string = replaceSystemProperty(string, "java.vendor"); // Java vendor specific string
    string = replaceSystemProperty(string, "java.vendor.url"); // Java vendor URL
    string = replaceSystemProperty(string, "java.vendor.version"); // Java vendor version
    string = replaceSystemProperty(string, "java.home"); // Java installation directory
    string = replaceSystemProperty(string, "java.class.version"); // Java class version number
    string = replaceSystemProperty(string, "java.class.path"); // Java classpath
    string = replaceSystemProperty(string, "os.name"); // Operating System Name
    string = replaceSystemProperty(string, "os.arch"); // Operating System Architecture
    string = replaceSystemProperty(string, "os.version"); // Operating System Version
    string = replaceSystemProperty(string, "file.separator"); // File separator ("/" on Unix)
    string = replaceSystemProperty(string, "path.separator"); // Path separator (":" on Unix)
    string = replaceSystemProperty(string, "line.separator"); // Line separator ("\n" on Unix)
    string = replaceSystemProperty(string, "user.name"); // User account name
    string = replaceSystemProperty(string, "user.home"); // User home directory
    string = replaceSystemProperty(string, "user.dir"); // User's current working directory
    // replace "future" system properties
    for (String property : OverlaySingleton.INSTANCE.systemPropertyNames()) {
      string = replaceSystemProperty(string, property);
    }
    return string;
  }

  private static String replaceSystemProperty(String string, String key) {
    if (string.indexOf('$') == -1) {
      return string;
    }
    String replacement = System.getProperty(key);
    if (replacement == null) {
      return string;
    }
    return string.replace("${" + key + "}", replacement);
  }
}
