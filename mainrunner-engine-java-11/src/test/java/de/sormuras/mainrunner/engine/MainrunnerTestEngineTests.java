package de.sormuras.mainrunner.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestEngine;

class MainrunnerTestEngineTests {

  @Test
  void checkEngineId() {
    TestEngine engine = new MainrunnerTestEngine();

    assertEquals("mainrunner", engine.getId());
  }
}
