import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.command.JavacCommand;
import com.github.sormuras.bach.external.JUnit;
import com.github.sormuras.bach.external.Maven;
import com.github.sormuras.bach.workflow.CompileWorkflow;
import com.github.sormuras.bach.workflow.WorkflowRunner;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.Set;

class build {

  static final String JUNIT_VERSION = "5.8.1";
  static final String ASSERTJ_VERSION = "3.21.0";

  public static void main(String... args) {
    try (var bach = new Bach(args)) {
      var project = project(bach);
      bach.logMessage("Build project %s".formatted(project.toNameAndVersion()));
      var runner = new WorkflowRunner(bach, project);
      runner.grabExternals();
      runner.run(
          new CompileWorkflow(bach, project, project.space("main")) {
            @Override
            protected JavacCommand computeJavacCommand(Path classes) {
              return super.computeJavacCommand(classes).add("-Xlint").add("-Werror");
            }
          },
          new CompileWorkflow(bach, project, project.space("test")) {
            @Override
            protected JavacCommand computeJavacCommand(Path classes) {
              return super.computeJavacCommand(classes).add("-encoding", "UTF-8");
            }
          });
      runner.executeTests();
    }
  }

  static Project project(Bach bach) {
    return Project.of(
            "mainrunner",
            bach.configuration()
                .projectOptions()
                .version()
                .orElse(ModuleDescriptor.Version.parse("2.2-ea")))
        .withSpaces(
            spaces ->
                spaces
                    .withSpace(
                        "main",
                        main ->
                            main.withRelease(17)
                                .withModule("com.github.sormuras.mainrunner.api/main/java")
                                .withModule(
                                    "com.github.sormuras.mainrunner.engine/main/java",
                                    engine ->
                                        engine.withResourcesFolder(
                                            "com.github.sormuras.mainrunner.engine/main/resources")))
                    .withSpace(
                        "test",
                        Set.of("main"),
                        test ->
                            test.withModule("test.integration/test/java")
                                .withModule("test.programs/test/java")))
        .withExternals(
            externals ->
                externals
                    .withExternalModuleLocator(JUnit.version(JUNIT_VERSION))
                    .withExternalModuleUri(
                        "org.assertj.core",
                        Maven.central("org.assertj", "assertj-core", ASSERTJ_VERSION)));
  }
}
