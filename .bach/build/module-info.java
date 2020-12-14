import com.github.sormuras.bach.project.Feature;
import com.github.sormuras.bach.project.ProjectInfo;
import com.github.sormuras.bach.project.ProjectInfo.Tweak;

@ProjectInfo(
    version = "2.2-ea",
    compileModulesForJavaRelease = 8,
    modules = {
      "com.github.sormuras.mainrunner.api/main/java-module/module-info.java",
      "com.github.sormuras.mainrunner.engine/main/java-module/module-info.java"
    },
    features = Feature.GENERATE_API_DOCUMENTATION,
    tests = {"test.integration/test/java/module-info.java", "test.programs/test/java/module-info.java"},
    tweaks = {
      @Tweak(tool = "javadoc", with = {"-encoding", "UTF-8"}),
      @Tweak(tool = "javadoc", with = {"-windowtitle", "🦄 Mainrunner"}),
      @Tweak(tool = "javadoc", with = {"-header", "🦄 Mainrunner"}),
      @Tweak(tool = "javadoc", with = {"-overview", ".bach/api-overview.html"}),
      @Tweak(tool = "javadoc", with = "-notimestamp")
    }
)
module build {
  requires com.github.sormuras.bach;
}
