package api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.sormuras.mainrunner.api.Main;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.platform.commons.support.AnnotationSupport;

class MainrunnerApiTests {

  @Test
  @Main
  void defaultMainValues(TestInfo info) {
    Method method = info.getTestMethod().orElseThrow(Error::new);
    List<Main> mains = AnnotationSupport.findRepeatableAnnotations(method, Main.class);

    assertEquals(1, mains.size());
    Main main = mains.get(0);

    assertEquals("[]", Arrays.toString(main.value()));
    assertEquals("main(${ARGS})", main.displayName());
    assertEquals(0, main.java().expectedExitValue());
    assertEquals("[]", Arrays.toString(main.java().options()));
  }
}
