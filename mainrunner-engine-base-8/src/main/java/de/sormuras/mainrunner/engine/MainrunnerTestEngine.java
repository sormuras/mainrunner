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
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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
import org.junit.platform.engine.discovery.UriSelector;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

/** Main-invoking TestEngine implementation. */
public class MainrunnerTestEngine implements TestEngine {

  private static Overlay OVERLAY = OverlaySingleton.INSTANCE;

  @Override
  public String getId() {
    return "mainrunner";
  }

  @Override
  public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
    EngineDescriptor engine = new EngineDescriptor(uniqueId, OVERLAY.display());

    ClassFilter classFilter = ClassFilter.of(buildClassNamePredicate(discoveryRequest), c -> true);
    Set<URI> uris = new HashSet<>();

    // class-path root
    discoveryRequest
        .getSelectorsByType(ClasspathRootSelector.class)
        .stream()
        .map(ClasspathRootSelector::getClasspathRoot)
        .peek(candidate -> handleCandidate(engine, uris, candidate))
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

    // uri
    if (OVERLAY.isSingleFileSourceCodeProgramExecutionSupported()) {
      discoveryRequest
          .getSelectorsByType(UriSelector.class)
          .stream()
          .map(UriSelector::getUri)
          .forEach(candidate -> handleCandidate(engine, uris, candidate));
    }

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

  private void handleCandidate(EngineDescriptor engine, Set<URI> uris, URI candidate) {
    if (!OVERLAY.isSingleFileSourceCodeProgramExecutionSupported()) {
      return;
    }
    if (uris.contains(candidate)) {
      return;
    }
    uris.add(candidate);
    Path path = Paths.get(candidate);
    if (Files.isDirectory(path)) {
      try {
        Files.find(path, 1, MainProgram::isSingleFileSourceCodeProgram)
            .map(Path::normalize)
            .map(Path::toAbsolutePath)
            .forEach(p -> handleCandidate(engine, p));
      } catch (IOException e) {
        throw new UncheckedIOException("scan directory failed: " + path, e);
      }
      return;
    }
    handleCandidate(engine, path);
  }

  private void handleCandidate(EngineDescriptor engine, Path program) {
    UniqueId idProgram = engine.getUniqueId().append("main-java", "java-" + program);
    MainProgram mainProgram = new MainProgram(idProgram, program);
    engine.addChild(mainProgram);

    UniqueId idTest = mainProgram.getUniqueId().append("main", "main0");
    mainProgram.addChild(new MainRun(idTest, program));
  }

  @Override
  public void execute(ExecutionRequest request) {
    TestDescriptor engine = request.getRootTestDescriptor();
    EngineExecutionListener listener = request.getEngineExecutionListener();
    listener.executionStarted(engine);
    for (TestDescriptor child : engine.getChildren()) {
      listener.executionStarted(child);
      if (child instanceof MainClass) {
        for (TestDescriptor mainMethod : child.getChildren()) {
          listener.executionStarted(mainMethod);
          TestExecutionResult result = executeMainMethod(((MainMethod) mainMethod));
          listener.executionFinished(mainMethod, result);
        }
      }
      if (child instanceof MainProgram) {
        for (TestDescriptor mainRun : child.getChildren()) {
          listener.executionStarted(mainRun);
          TestExecutionResult result = executeMainRun(((MainRun) mainRun));
          listener.executionFinished(mainRun, result);
        }
      }
      listener.executionFinished(child, TestExecutionResult.successful());
    }
    listener.executionFinished(engine, TestExecutionResult.successful());
  }

  private static TestExecutionResult executeMainMethod(MainMethod mainMethod) {
    return mainMethod.isFork() ? executeForked(mainMethod) : executeDirect(mainMethod);
  }

  private static TestExecutionResult executeDirect(MainMethod mainMethod) {
    try {
      Method method = mainMethod.getMethod();
      Object[] arguments = new Object[] {mainMethod.getArguments()};
      method.invoke(null, arguments);
    } catch (Throwable t) {
      return TestExecutionResult.failed(t);
    }
    return TestExecutionResult.successful();
  }

  private static TestExecutionResult executeForked(MainMethod mainMethod) {
    List<String> arguments = new ArrayList<>();
    Arrays.stream(mainMethod.getOptions())
        .map(MainrunnerTestEngine::replaceSystemProperties)
        .forEach(arguments::add);
    arguments.add(mainMethod.getMethod().getDeclaringClass().getName());
    arguments.addAll(Arrays.asList(mainMethod.getArguments()));
    return start(null, arguments, mainMethod.getExpectedExitValue());
  }

  private static TestExecutionResult executeMainRun(MainRun mainTest) {
    Path path = mainTest.getPath();
    return start(path.getParent(), Collections.singletonList(path.toString()), 0);
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
    for (String property : OVERLAY.systemPropertyNames()) {
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

  private static TestExecutionResult start(Path directory, List<String> arguments, int expected) {
    String java = OVERLAY.java().normalize().toAbsolutePath().toString();
    ProcessBuilder builder = new ProcessBuilder(java);
    builder.inheritIO();
    builder.command().addAll(arguments);
    if (directory != null) {
      builder.directory(directory.toFile());
    }
    try {
      Process process = builder.start();
      try {
        if (!process.waitFor(3, TimeUnit.MINUTES)) {
          // timedOut = true;
          process.destroy();
          // give process a second to terminate normally
          for (int i = 10; i > 0 && process.isAlive(); i--) {
            Thread.sleep(123);
          }
          // if the process is still alive, kill it
          if (process.isAlive()) {
            process.destroyForcibly();
            for (int i = 10; i > 0 && process.isAlive(); i--) {
              Thread.sleep(1234);
            }
          }
        }
        if (process.isAlive()) {
          throw new RuntimeException("process is still alive: " + process);
        }
        if (process.exitValue() != expected) {
          String message = "expected exit value " + expected + ", but got: " + process.exitValue();
          return TestExecutionResult.failed(new IllegalStateException(message));
        }
      } catch (InterruptedException e) {
        throw new RuntimeException("run failed", e);
      }
    } catch (IOException e) {
      return TestExecutionResult.failed(e);
    }
    return TestExecutionResult.successful();
  }
}
