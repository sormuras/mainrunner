/*
 * Mainrunner Build Program
 * Copyright (C) 2019 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// default package

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BuildMainrunner {

  public static void main(String... args) {
    System.out.println("BEGIN");
    var out = new PrintWriter(System.out, true);
    var err = new PrintWriter(System.err, true);
    var mainrunner = project();
    var bach = new Bach(out, err, true, mainrunner);

    bach.build();
    deploy(mainrunner);

    System.out.println("END.");
  }

  private static Bach.Project project() {
    var api =
        new Bach.Project.MultiReleaseUnit(
            Bach.Project.ModuleInfoReference.of(
                Path.of("src/de.sormuras.mainrunner.api/main/java-9/module-info.java")),
            9,
            Map.of(
                8,
                Path.of("src/de.sormuras.mainrunner.api/main/java-8"),
                9,
                Path.of("src/de.sormuras.mainrunner.api/main/java-9")),
            List.of());
    var engine =
        new Bach.Project.MultiReleaseUnit(
            Bach.Project.ModuleInfoReference.of(
                Path.of("src/de.sormuras.mainrunner.engine/main/java-9/module-info.java")),
            9,
            Map.of(
                8,
                Path.of("src/de.sormuras.mainrunner.engine/main/java-8"),
                9,
                Path.of("src/de.sormuras.mainrunner.engine/main/java-9"),
                11,
                Path.of("src/de.sormuras.mainrunner.engine/main/java-11")),
            List.of(Path.of("src/de.sormuras.mainrunner.engine/main/resources")));
    var main =
        new Bach.Project.Realm(
            "main",
            false,
            11,
            String.join(File.separator, "src", "*", "main", "java-9"),
            List.of(api, engine));

    var programs =
        new Bach.Project.ModuleSourceUnit(
            Bach.Project.ModuleInfoReference.of(Path.of("src/programs/test/java/module-info.java")),
            List.of(Path.of("src/programs/test/java")),
            List.of());
    var integration =
        new Bach.Project.ModuleSourceUnit(
            Bach.Project.ModuleInfoReference.of(
                Path.of("src/integration/test/java/module-info.java")),
            List.of(Path.of("src/integration/test/java")),
            List.of());
    var test =
        new Bach.Project.Realm(
            "test",
            false,
            0,
            String.join(File.separator, "src", "*", "test", "java"),
            List.of(programs, integration),
            main);

    var library = new Bach.Project.Library(Path.of("lib"));
    return new Bach.Project(
        Path.of(""),
        Path.of("bin"),
        "mainrunner",
        ModuleDescriptor.Version.parse("2.1.5"),
        library,
        List.of(main, test));
  }

  private static void deploy(Bach.Project mainrunner) {
    var plugin = "org.apache.maven.plugins:maven-deploy-plugin:3.0.0-M1:deploy-file";
    var repository = "-DrepositoryId=bintray-sormuras-maven";
    var url = "-Durl=https://api.bintray.com/maven/sormuras/maven/mainrunner/;publish=0"; // 1
    var maven = String.join(" ", "call", "mvn", plugin, repository, url);
    var lines = new ArrayList<String>();
    var main = mainrunner.realms.get(0);
    var version = mainrunner.version;
    for (var unit : main.units) {
      var module = unit.name();
      var moduleDashVersion = module + "-" + version;
      var path = Path.of("bin", "realm", main.name);
      var pom = "-DpomFile=" + Path.of("src", module, "maven", "pom.xml");
      var file = "-Dfile=" + path.resolve("modules").resolve(moduleDashVersion + ".jar");
      var sources = "-Dsources=" + path.resolve(moduleDashVersion + "-sources.jar");
      var javadoc = "-Djavadoc=" + path.resolve("mainrunner-" + version + "-javadoc.jar");
      lines.add(String.join(" ", maven, pom, file, sources, javadoc));
    }
    try {
      Files.write(Path.of("bin/deploy.bat"), lines);
    } catch (IOException e) {
      throw new UncheckedIOException("Deploy failed: " + e.getMessage(), e);
    }
  }
}
