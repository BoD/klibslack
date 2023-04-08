plugins {
  kotlin("jvm")
}

dependencies {
  // Kotlin
  implementation(KotlinX.coroutines.jdk9)

  // Slf4j
  implementation("org.slf4j:slf4j-simple:_")

  // Library
  implementation(project(":klibslack"))
}
