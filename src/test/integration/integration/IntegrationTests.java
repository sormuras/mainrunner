package integration;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineTestKit;

public class IntegrationTests {

  public static void main(String[] args) {
    // throw new Error("123");
  }

  @Test
  void test() {}

  @Test
  void verifyMainrunnerStatistics() {
    EngineTestKit.engine("mainrunner")
        .selectors(DiscoverySelectors.selectClass(Program.class))
        .execute()
        .tests()
        .assertStatistics(stats -> stats.skipped(0).started(1).succeeded(1).aborted(0).failed(0));
  }
}
