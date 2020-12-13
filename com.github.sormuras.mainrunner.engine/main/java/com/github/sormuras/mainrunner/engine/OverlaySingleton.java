package com.github.sormuras.mainrunner.engine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;

enum OverlaySingleton implements Overlay {
  INSTANCE;

  @Override
  public String display() {
    return "Mainrunner (Java 8)";
  }

  @Override
  public boolean isSingleFileSourceCodeProgramExecutionSupported() {
    return false;
  }

  @Override
  public String readString(Path path) throws IOException {
    return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
  }
}
