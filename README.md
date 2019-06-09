![mainrunner](mainrunner.png)

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
