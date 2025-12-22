plugins {
  kotlin("jvm")
}

dependencies {
  // Kotlin
  implementation(KotlinX.coroutines.jdk9)

  // Logging
  implementation("org.jraf.klibnanolog:klibnanolog:_")

  // Library
  implementation(project(":klibslack"))
}
