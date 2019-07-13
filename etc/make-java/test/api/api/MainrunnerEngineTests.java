package api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MainrunnerEngineTests {

  @Test
  void accessingMainrunnerViaReflectionIsIllegal() throws Exception {
    var mainrunnerClass = Class.forName("de.sormuras.mainrunner.engine.Mainrunner");
    var exception =
        Assertions.assertThrows(
            IllegalAccessException.class, () -> mainrunnerClass.getConstructor().newInstance());
    Assertions.assertEquals(
        "class api.MainrunnerEngineTests (in module api)"
            + " cannot access class de.sormuras.mainrunner.engine.Mainrunner (in module de.sormuras.mainrunner.engine)"
            + " because module de.sormuras.mainrunner.engine does not export de.sormuras.mainrunner.engine to module api",
        exception.getMessage());
  }
}
