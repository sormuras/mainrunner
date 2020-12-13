package test.integration;

import com.github.sormuras.mainrunner.api.Main;
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
            DiscoverySelectors.selectModule("test.programs"))
        .filters(ClassNameFilter.includeClassNamePatterns(".*Program"))
        .execute()
        .testEvents()
        .assertStatistics(stats -> stats.skipped(0).started(8).succeeded(7).aborted(0).failed(1));
  }

  public static class StaticNestedClassProgram {
    public static void main(String[] args) {
      assert args.length == 0;
    }
  }

  public static class AnnotatedProgram {
    @Main // 0
    @Main("1") // 1
    @Main({"1", "2"}) // 2
    public static void main(String[] args) {
      assert args.length <= 2;
    }
  }
}
