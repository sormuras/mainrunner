import com.github.sormuras.bach.ProjectInfo;
import com.github.sormuras.bach.ProjectInfo.External;
import com.github.sormuras.bach.ProjectInfo.Externals;
import com.github.sormuras.bach.ProjectInfo.Tools;
import com.github.sormuras.bach.ProjectInfo.Tweak;

@ProjectInfo(
    compileModulesForJavaRelease = 8,
    lookupExternal = @External(module = "org.assertj.core", via = "org.assertj:assertj-core:3.19.0"),
    lookupExternals = @Externals(name = Externals.Name.JUNIT, version = "5.8.0-M1"),
    tools = @Tools(skip = "jlink"),
    tweaks = {
      @Tweak(tool = "javadoc", option = "-encoding", value = "UTF-8"),
      @Tweak(tool = "javadoc", option = "-windowtitle", value = "🦄 Mainrunner"),
      @Tweak(tool = "javadoc", option = "-header", value = "🦄 Mainrunner"),
      @Tweak(tool = "javadoc", option = "-doctitle", value = "🦄 Mainrunner 2.2-ea"),
      @Tweak(tool = "javadoc", option = "-overview", value = ".bach/api-overview.html"),
      @Tweak(tool = "javadoc", option = "-notimestamp")
    }
)
module bach.info {
  requires com.github.sormuras.bach;
}
