package de.sormuras.mainrunner.engine;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Pattern;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.FileSource;

class MainProgram extends AbstractTestDescriptor {

  private static String JAVA_PROGRAM_REGEX = ".+(?:public\\s+)?static\\s+void\\s+main.+String.+";
  private static Pattern JAVA_PROGRAM_PATTERN = Pattern.compile(JAVA_PROGRAM_REGEX, Pattern.DOTALL);

  static boolean isSingleFileSourceCodeProgram(String code) {
    return JAVA_PROGRAM_PATTERN.matcher(code).matches();
  }

  static boolean isSingleFileSourceCodeProgram(Path path, BasicFileAttributes attributes) {
    if (!attributes.isRegularFile()) {
      return false;
    }
    if (attributes.size() < "interface A{static void main(String[]a){}}".length()) {
      return false;
    }
    if (!path.toString().endsWith(".java")) {
      return false;
    }
    try {
      String code = OverlaySingleton.INSTANCE.readString(path);
      return isSingleFileSourceCodeProgram(code);
    } catch (IOException e) {
      return false;
    }
  }

  MainProgram(UniqueId uniqueId, Path path) {
    super(uniqueId, path.getFileName().toString(), FileSource.from(path.toFile()));
  }

  @Override
  public Type getType() {
    return Type.CONTAINER;
  }
}
