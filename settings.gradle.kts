rootProject.name = "klibslack-root"

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  @Suppress("UnstableApiUsage")
  repositories {
    mavenCentral()
  }
}

plugins {
  // See https://splitties.github.io/refreshVersions/
  id("de.fayard.refreshVersions") version "0.60.6"
}

include(":library")
project(":library").name = "klibslack"

// Include all the sample modules from the "samples" directory
file("samples").listFiles()!!.forEach { dir ->
  include(dir.name)
  project(":${dir.name}").apply {
    projectDir = dir
    name = dir.name
  }
}
