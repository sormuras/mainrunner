import com.github.sormuras.bach.project.ProjectInfo;

@ProjectInfo(
    version = "2.2-ea",
    modules = {
      "de.sormuras.mainrunner.api/main/java-module/module-info.java",
      "de.sormuras.mainrunner.engine/main/java-module/module-info.java"
    },
    compileModulesForJavaRelease = 8,
    tests = {
        "integration/test/java/module-info.java",
        "programs/test/java/module-info.java"
    })
module build {
  requires com.github.sormuras.bach;
}
