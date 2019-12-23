import de.sormuras.bach.Bach;
import de.sormuras.bach.project.Configuration;

class Build {
  public static void main(String[] args) {
    // Bach.build("mainrunner", "2.1.6");
    Bach.build(Configuration.of("mainrunner", "2.1.6").setGroup("de.sormuras.mainrunner"));
  }
}
