![mainrunner](mainrunner.png)

[![jdk8](https://img.shields.io/badge/java-8-lightgray.svg)](http://jdk.java.net/8)
[![jdk11](https://img.shields.io/badge/java-11+-blue.svg)](http://jdk.java.net/11)
[![github actions](https://github.com/sormuras/mainrunner/workflows/CI/badge.svg)](https://github.com/sormuras/mainrunner/actions)
[![experimental](https://img.shields.io/badge/api-experimental-yellow.svg)](https://javadoc.io/doc/de.sormuras.mainrunner/de.sormuras.mainrunner.engine)
[![central](https://img.shields.io/maven-central/v/de.sormuras.mainrunner/de.sormuras.mainrunner.engine.svg)](https://search.maven.org/search?q=g:de.sormuras.mainrunner%20AND%20a:de.sormuras.mainrunner.*)

JUnit Platform Test Engine launching Java programs


### Usage

1. Write a plain Java program under `src/test/java`
2. Include Mainrunner at test runtime

#### Gradle Kotlin DSL snippets


```kotlin
dependencies {
    testImplementation("de.sormuras.mainrunner:de.sormuras.mainrunner.engine:$VERSION") {
        because("executes Java programs as tests")
    }
}
```

Only needed for versions not available on Maven Central
```kotlin
repositories {
    mavenCentral()
    maven(url = "https://dl.bintray.com/sormuras/maven") {
        content {
            includeGroup("de.sormuras.mainrunner")
        }
    }
}
```
