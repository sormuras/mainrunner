VERSION='2.1.6'
BIN='bin/realm/main'
mvn install:install-file -Dpackaging=jar -DpomFile=src/de.sormuras.mainrunner.api/maven/pom.xml -Dfile=${BIN}/modules/de.sormuras.mainrunner.api-${VERSION}.jar -Dsources=${BIN}/de.sormuras.mainrunner.api-${VERSION}-sources.jar -Djavadoc=${BIN}/mainrunner-${VERSION}-javadoc.jar
mvn install:install-file -Dpackaging=jar -DpomFile=src/de.sormuras.mainrunner.engine/maven/pom.xml -Dfile=${BIN}/modules/de.sormuras.mainrunner.engine-${VERSION}.jar -Dsources=${BIN}/de.sormuras.mainrunner.engine-${VERSION}-sources.jar -Djavadoc=${BIN}/mainrunner-${VERSION}-javadoc.jar
