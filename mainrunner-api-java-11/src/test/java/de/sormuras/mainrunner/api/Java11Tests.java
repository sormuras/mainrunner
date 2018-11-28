package de.sormuras.mainrunner.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.platform.commons.support.AnnotationSupport;

class Java11Tests {

  @Test
  void accessJava11() {
    assertEquals("Java11", Java11.class.getSimpleName());
  }

  @Test
  @Main
  void defaultMainValues(TestInfo info) {
    var method = info.getTestMethod().orElseThrow(Error::new);
    var mains = AnnotationSupport.findRepeatableAnnotations(method, Main.class);

    assertEquals(1, mains.size());
    var main = mains.get(0);

    assertEquals("[]", Arrays.toString(main.value()));
    assertEquals("main(${ARGS})", main.displayName());
    assertEquals(0, main.java().expectedExitValue());
    assertEquals("[]", Arrays.toString(main.java().options()));
  }
}
