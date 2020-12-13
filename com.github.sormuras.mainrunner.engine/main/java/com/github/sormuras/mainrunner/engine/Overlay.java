package com.github.sormuras.mainrunner.engine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

interface Overlay {

  String display();

  boolean isSingleFileSourceCodeProgramExecutionSupported();

  String readString(Path path) throws IOException;
}
