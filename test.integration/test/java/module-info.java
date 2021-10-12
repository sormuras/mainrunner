open module test.integration {
  requires com.github.sormuras.mainrunner.api;
  requires com.github.sormuras.mainrunner.engine;

  requires org.junit.jupiter;
  requires org.junit.platform.console;
  requires org.junit.platform.launcher;
  requires org.junit.platform.testkit;
  requires org.assertj.core;
  requires test.programs;

  provides java.util.spi.ToolProvider with
      test.integration.ModularTestLauncher;
}
