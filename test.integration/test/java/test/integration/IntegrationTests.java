package test.integration;

import com.github.sormuras.mainrunner.api.Run;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.ClassNameFilter;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineTestKit;

public class IntegrationTests {

  public static void main(String[] args) {}

  @Test
  void verifyMainrunnerStatistics() {
    EngineTestKit.engine("mainrunner")
        .selectors(
            DiscoverySelectors.selectClass(StaticNestedClassProgram.class),
            DiscoverySelectors.selectClass(AnnotatedProgram.class),
            DiscoverySelectors.selectModule("test.programs"))
        .filters(ClassNameFilter.includeClassNamePatterns(".*Program"))
        .execute()
        .testEvents()
        .assertStatistics(stats -> stats.skipped(0).started(11).succeeded(10).aborted(0).failed(1));
  }

  public static class StaticNestedClassProgram {
    public static void main(String[] args) {
      assert args.length == 0;
    }
  }

  public static class AnnotatedProgram {
    @Run // 0
    @Run("1") // 1
    @Run({"1", "2"}) // 2
    public static void main(String[] args) {
      assert args.length <= 2;
    }
  }
}
