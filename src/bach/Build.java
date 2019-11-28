import de.sormuras.bach.Bach;
import de.sormuras.bach.Log;
import de.sormuras.bach.project.Deployment;
import de.sormuras.bach.project.Folder;
import de.sormuras.bach.project.Project;
import de.sormuras.bach.project.ProjectBuilder;
import java.lang.module.ModuleDescriptor.Version;
import java.net.URI;

public class Build {
  public static void main(String[] args) {
    System.out.println("Build.main BEGIN");
    var log = Log.ofSystem(true);
    var builder = new ProjectBuilder(log);
    var folder = Folder.of();
    var version = Version.parse(builder.properties(folder).getProperty("version"));
    var structure = builder.structure(folder);
    var deployment =
        new Deployment(
            "bintray-sormuras-maven",
            URI.create("https://api.bintray.com/maven/sormuras/maven/mainrunner/;publish=1"));
    Bach.build(log, new Project("mainrunner", version, structure, deployment));
    System.out.println("Build.main END.");
  }
}
