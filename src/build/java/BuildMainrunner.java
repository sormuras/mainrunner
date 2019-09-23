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
            Path.of("src/de.sormuras.mainrunner.api/java-9/module-info.java"),
            9,
            Map.of(
                8,
                Path.of("src/de.sormuras.mainrunner.api/main/java-8"),
                9,
                Path.of("src/de.sormuras.mainrunner.api/main/java-9")),
            List.of(),
            ModuleDescriptor.newModule("de.sormuras.mainrunner.api").build());
    var engine =
        new Bach.Project.MultiReleaseUnit(
            Path.of("src/de.sormuras.mainrunner.engine/java-9/module-info.java"),
            9,
            Map.of(
                8,
                Path.of("src/de.sormuras.mainrunner.engine/main/java-8"),
                9,
                Path.of("src/de.sormuras.mainrunner.engine/main/java-9"),
                11,
                Path.of("src/de.sormuras.mainrunner.engine/main/java-11")),
            List.of(Path.of("src/de.sormuras.mainrunner.engine/main/resources")),
            ModuleDescriptor.newModule("de.sormuras.mainrunner.engine").build());
    var main =
        new Bach.Project.Realm(
            "main",
            false,
            11,
            String.join(File.separator, "src", "*", "main", "java-9"),
            Map.of("hydra", List.of(api.descriptor.name(), engine.descriptor.name())),
            Map.of(api.descriptor.name(), api, engine.descriptor.name(), engine));
    var library = new Bach.Project.Library(List.of(Path.of("lib")), __ -> null);
    return new Bach.Project(
        Path.of(""),
        Path.of("bin"),
        "mainrunner",
        ModuleDescriptor.Version.parse("2.1.3"),
        library,
        List.of(main));
  }

  private static void deploy(Bach.Project mainrunner) {
    var plugin = "org.apache.maven.plugins:maven-deploy-plugin:3.0.0-M1:deploy-file";
    var repository = "-DrepositoryId=bintray-sormuras-maven";
    var url = "-Durl=https://api.bintray.com/maven/sormuras/maven/mainrunner/;publish=0"; // 1
    var maven = String.join(" ", "call", "mvn", plugin, repository, url);
    var lines = new ArrayList<String>();
    var root = Path.of("src/build/maven/pom.xml");
    lines.add(String.join(" ", maven, "-DpomFile=" + root, "-Dfile=" + root));
    var main = mainrunner.realms.get(0);
    for (var unit : main.units.values()) {
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
