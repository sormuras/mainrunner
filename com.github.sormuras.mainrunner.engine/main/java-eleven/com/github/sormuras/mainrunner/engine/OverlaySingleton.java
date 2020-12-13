package com.github.sormuras.mainrunner.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

enum OverlaySingleton implements Overlay {
  INSTANCE;

  @Override
  public String display() {
    var module = getClass().getModule();
    var descriptor = module.getDescriptor();
    return "Mainrunner (" + (descriptor != null ? descriptor.toNameAndVersion() : module) + ")";
  }

  @Override
  public boolean isSingleFileSourceCodeProgramExecutionSupported() {
    return true;
  }

  @Override
  public String readString(Path path) throws IOException {
    return Files.readString(path);
  }
}
