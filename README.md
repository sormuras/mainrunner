![mainrunner](mainrunner.png)

[![jdk8](https://img.shields.io/badge/java-8-lightgray.svg)](http://jdk.java.net/8)
[![jdk11](https://img.shields.io/badge/java-11+-blue.svg)](http://jdk.java.net/11)
[![travis](https://travis-ci.com/sormuras/mainrunner.svg?branch=master)](https://travis-ci.com/sormuras/mainrunner)
[![experimental](https://img.shields.io/badge/api-experimental-yellow.svg)](https://javadoc.io/doc/de.sormuras.mainrunner/de.sormuras.mainrunner.engine)
[![central](https://img.shields.io/maven-central/v/de.sormuras.mainrunner/de.sormuras.mainrunner.engine.svg)](https://search.maven.org/search?q=g:de.sormuras.mainrunner%20AND%20a:de.sormuras.mainrunner.*)

JUnit Platform Test Engine launching Java programs


### Usage

1. Write a plain Java program
2. Include Mainrunner at test runtime

#### Gradle Kotlin DSL snippets

```kotlin
repositories {
    mavenCentral()
    // Only needed for versions not available on Maven Central
    maven(url = "https://dl.bintray.com/sormuras/maven") {
        content {
            includeGroup("de.sormuras.mainrunner")
        }
    }
}

dependencies {
    testImplementation("de.sormuras.mainrunner:de.sormuras.mainrunner.engine:$VERSION") {
        because("executes Java programs as tests")
    }
}
```
