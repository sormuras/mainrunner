/**
 * Provides {@code TestEngine} implementation that runs Java programs as tests.
 *
 * <p>This module doesn't export any package on purpose.
 */
module com.github.sormuras.mainrunner.engine {
  requires static com.github.sormuras.mainrunner.api;
  requires org.junit.platform.engine;

  provides org.junit.platform.engine.TestEngine with
      com.github.sormuras.mainrunner.engine.Mainrunner;
}
