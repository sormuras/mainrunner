open module integration {
  requires de.sormuras.mainrunner.api;

  requires programs;
  requires org.junit.jupiter;
  requires org.junit.platform.console;
  requires org.junit.platform.launcher;
  requires org.junit.platform.testkit;
  requires org.assertj.core;

  provides java.util.spi.ToolProvider with
      integration.ModularTestLauncher;
}
