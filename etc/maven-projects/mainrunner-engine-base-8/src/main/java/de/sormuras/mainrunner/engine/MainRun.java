package de.sormuras.mainrunner.engine;

import java.nio.file.Path;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.FileSource;

class MainRun extends AbstractTestDescriptor {

  private final Path path;

  MainRun(UniqueId uniqueId, Path path) {
    super(uniqueId, "main()", FileSource.from(path.toFile()));
    this.path = path;
  }

  Path getPath() {
    return path;
  }

  @Override
  public Type getType() {
    return Type.TEST;
  }
}
