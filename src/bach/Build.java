import de.sormuras.bach.Bach;
import de.sormuras.bach.Log;
import de.sormuras.bach.project.Configuration;

class Build {
  public static void main(String[] args) {
    // Bach.build("mainrunner", "2.1.6");
    Bach.build(
        Log.ofSystem(),
        Configuration.of("mainrunner", "2.1.6")
            .setDeployment(
                "de.sormuras.mainrunner",
                "bintray-sormuras-maven",
                "https://api.bintray.com/maven/sormuras/maven/mainrunner/;publish=1"));
  }
}
