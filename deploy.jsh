/open https://github.com/sormuras/bach/raw/master/BUILDING

var module="de.sormuras.mainrunner.engine"
var version="2-ea"
var args = new Arguments()
args.add("org.apache.maven.plugins:maven-deploy-plugin:3.0.0-M1:deploy-file")
args.add("-DrepositoryId=bintray-sormuras-maven")
args.add("-Durl=https://api.bintray.com/maven/sormuras/maven/mainrunner/;publish=1")
args.add("-DpomFile=" + Path.of("src", "poms", module, "pom.xml"))
args.add("-Dfile=" + Path.of("target/main/modules", module + '-' + version + ".jar"))
args.add("-Dsources=" + Path.of("target/main/sources", module + '-' + version + "-sources.jar"))
args.add("-Djavadoc=" + Path.of("target/main/javadoc", "mainrunner-" + version + "-javadoc.jar"))

/exit exe("mvn.cmd", args.toArray())
