package build;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Command;
import de.sormuras.bach.Jigsaw;
import de.sormuras.bach.Project;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BuildMainrunner {

  public static void main(String[] args) {
    System.out.println("BEGIN");
    System.setProperty("ebug", "");
    var bach = Bach.of();
    var mainrunner = project();
    var jigsaw = new Jigsaw(bach, mainrunner);
    var commands =
        jigsaw.toCommands(
            mainrunner.realms.get(0),
            List.of("de.sormuras.mainrunner.api", "de.sormuras.mainrunner.engine"));

    //    for (var command : commands) {
    //      System.out.println(command.toCommandLine());
    //      var code =
    //          ToolProvider.findFirst(command.getName())
    //              .orElseThrow()
    //              .run(System.out, System.err, command.toStringArray());
    //      if (code != 0) {
    //        throw new AssertionError("Expected 0, but got: " + code);
    //      }
    //    }

    try {
      Files.write(
          Path.of("bin/build.bat"),
          commands.stream()
              .map(Command::toCommandLine)
              .peek(System.out::println)
              .collect(Collectors.toList()));
    } catch (IOException e) {
      throw new UncheckedIOException("Writing build batch file failed", e);
    }

    deploy(mainrunner);

    System.out.println("END.");
  }

  private static Project project() {
    var api =
        new Project.ModuleUnit(
            Path.of("src/de.sormuras.mainrunner.api/java-9/module-info.java"),
            List.of(
                Path.of("src/de.sormuras.mainrunner.api/main/java-8"),
                Path.of("src/de.sormuras.mainrunner.api/main/java-9")),
            List.of(),
            ModuleDescriptor.newModule("de.sormuras.mainrunner.api").build());
    var engine =
        new Project.ModuleUnit(
            Path.of("src/de.sormuras.mainrunner.engine/java-9/module-info.java"),
            List.of(
                Path.of("src/de.sormuras.mainrunner.engine/main/java-8"),
                Path.of("src/de.sormuras.mainrunner.engine/main/java-9")),
            List.of(Path.of("src/de.sormuras.mainrunner.engine/main/resources")),
            ModuleDescriptor.newModule("de.sormuras.mainrunner.engine").build());
    var main =
        new Project.Realm(
            "main",
            String.join(
                File.pathSeparator,
                String.join(File.separator, "src", "*", "main", "java-8"),
                String.join(File.separator, "src", "*", "main", "java-9")),
            Map.of(api.descriptor.name(), api, engine.descriptor.name(), engine));
    var library = new Project.Library(List.of(Path.of("lib")), __ -> null);
    return new Project(
        Path.of(""), Path.of("bin"), "mainrunner", Version.parse("2.1.3"), library, List.of(main));
  }

  private static void deploy(Project mainrunner) {
    var plugin = "org.apache.maven.plugins:maven-deploy-plugin:3.0.0-M1:deploy-file";
    var repository = "-DrepositoryId=bintray-sormuras-maven";
    var url = "-Durl=https://api.bintray.com/maven/sormuras/maven/mainrunner/;publish=0"; // 1
    var maven = String.join(" ", "call", "mvn", plugin, repository, url);
    var lines = new ArrayList<String>();
    var root = Path.of("src/build/maven/pom.xml");
    lines.add(String.join(" ", maven, "-DpomFile=" + root, "-Dfile=" + root));
    var main = mainrunner.realms.get(0);
    for (var unit : main.modules.values()) {
      var name = unit.descriptor.name();
      var path = Path.of("bin", "realm", main.name);
      var nameDashVersion = name + "-" + mainrunner.version;
      var pom = "-DpomFile=" + Path.of("src", name, "maven", "pom.xml");
      var file = "-Dfile=" + path.resolve("modules").resolve(nameDashVersion + ".jar");
      var sources =
          "-Dsources=" + path.resolve("sources").resolve(nameDashVersion + "-sources.jar");
      var javadoc = "-Djavadoc=" + path.resolve("javadoc").resolve("all-javadoc.jar");
      lines.add(String.join(" ", maven, pom, file, sources, javadoc));
    }
    try {
      Files.write(Path.of("bin/deploy.bat"), lines);
    } catch (IOException e) {
      throw new UncheckedIOException("Deploy failed: " + e.getMessage(), e);
    }
  }
}
