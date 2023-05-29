allprojects {
  repositories {
    mavenCentral()
  }

  group = "org.jraf.klibslack"
  version = "1.0.0"
}

plugins {
  kotlin("multiplatform").apply(false)
}

// `./gradlew refreshVersions` to update dependencies
