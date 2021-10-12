import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.external.JUnit;
import com.github.sormuras.bach.simple.SimpleSpace;

class build {
  public static void main(String... args) {
    try (var bach = new Bach(args)) {
      bach.logCaption("Compile main modules");
      var main =
          SimpleSpace.of(bach, "main")
              .withModule("com.github.sormuras.mainrunner.api")
              .withModule("com.github.sormuras.mainrunner.engine");
      var options = bach.configuration().projectOptions();

      main.compile(
          javac -> javac.add("-Xlint").add("-Werror").add("--release", 17),
          jar ->
              jar.verbose(true)
                  .add(
                      "--module-version",
                      options.version().map(Object::toString).orElse("2.2-ea")));

      bach.logCaption("Download external 3rd-party modules");
      var grabber = bach.grabber(JUnit.version("5.8.1"), build::locateExternalModule);
      grabber.grabExternalModules(
          // JUnit Jupiter
          "org.junit.jupiter",
          "org.junit.jupiter.api",
          "org.junit.jupiter.engine",
          "org.junit.jupiter.params",
          // JUnit Platform and friends
          "org.apiguardian.api",
          "org.assertj.core",
          "org.junit.platform.commons",
          "org.junit.platform.console",
          "org.junit.platform.engine",
          "org.junit.platform.launcher",
          "org.junit.platform.reporting",
          "org.junit.platform.testkit",
          "org.opentest4j");

      bach.logCaption("Perform automated checks");
      var test =
          main.newDependentSpace("test").withModule("test.integration").withModule("test.programs");

      test.compile(javac -> javac.add("-g").add("-parameters").add("-encoding", "UTF-8"));
      test.runAllTests();
    }
  }

  static String locateExternalModule(String module) {
    return switch (module) {
      case "org.assertj.core" -> "https://repo.maven.apache.org/maven2/org/assertj/assertj-core/3.21.0/assertj-core-3.21.0.jar";
      default -> null;
    };
  }
}
