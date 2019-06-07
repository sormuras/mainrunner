package de.sormuras.mainrunner.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

class MainClassTests {

  @Test
  void defaultMainClassValues() {
    Class<?> test = Test.class;
    UniqueId uniqueId = UniqueId.forEngine("XXX");
    TestDescriptor descriptor = new EngineDescriptor(uniqueId, "YYY");
    MainClass mainClass = MainClass.of(test, descriptor);

    String expectedUniqueId = "[engine:XXX]/[main-class:" + test.getName() + "]";

    assertEquals(test.getName(), mainClass.getDisplayName());
    assertEquals(test.getName(), mainClass.getLegacyReportingName());
    assertEquals(Collections.emptySet(), mainClass.getTags());
    assertEquals(TestDescriptor.Type.CONTAINER, mainClass.getType());
    assertEquals(expectedUniqueId, mainClass.getUniqueId().toString());
  }
}
