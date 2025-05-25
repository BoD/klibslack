plugins {
  kotlin("multiplatform")
  kotlin("plugin.serialization")
  id("maven-publish")
  id("org.jetbrains.dokka")
  id("signing")
}

// Generate a Version.kt file with a constant for the version name
val generateVersionKtTask = tasks.register("generateVersionKt") {
  val outputDir = layout.buildDirectory.dir("generated/source/kotlin").get().asFile
  outputs.dir(outputDir)
  doFirst {
    val outputWithPackageDir = File(outputDir, "org/jraf/klibslack/internal").apply { mkdirs() }
    File(outputWithPackageDir, "Version.kt").writeText(
      """
        package org.jraf.klibslack.internal
        internal const val VERSION = "${rootProject.version}"
      """.trimIndent()
    )
  }
}

// Generate Javadoc (Dokka) Jar
tasks.register<Jar>("dokkaHtmlJar") {
  archiveClassifier.set("javadoc")
  from("${layout.buildDirectory}/dokka")
  dependsOn(tasks.dokkaGenerate)
}

kotlin {
  jvm()
  jvmToolchain(11)

  sourceSets {
    commonMain {
      kotlin.srcDir(generateVersionKtTask)

      dependencies {
        // Ktor
        implementation(Ktor.client.core)
        implementation(Ktor.client.contentNegotiation)
        implementation(Ktor.client.auth)
        implementation(Ktor.client.logging)
        implementation("io.ktor:ktor-client-websockets:_")
        implementation(Ktor.plugins.serialization.kotlinx.json)

        // Serialization
        implementation(KotlinX.serialization.json)
      }
    }

    jvmMain {
      dependencies {
        // Slf4j
        implementation("org.slf4j:slf4j-api:_")
        implementation("org.slf4j:slf4j-simple:_")

        // Coroutines
        implementation(KotlinX.coroutines.jdk9)

        // Ktor OkHttp
        implementation(Ktor.client.okHttp)
      }
    }
  }
}

publishing {
  repositories {
    maven {
      // Note: declare your user name / password in your home's gradle.properties like this:
      // mavenCentralNexusUsername = <user name>
      // mavenCentralNexusPassword = <password>
      url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
      name = "mavenCentralNexus"
      credentials(PasswordCredentials::class)
    }
  }

  publications.withType<MavenPublication>().forEach { publication ->
    publication.artifact(tasks.getByName("dokkaHtmlJar"))

    publication.pom {
      name.set("klibslack")
      description.set("A Slack API client library for Kotlin.")
      url.set("https://github.com/BoD/klibslack")
      licenses {
        license {
          name.set("The Apache License, Version 2.0")
          url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
          distribution.set("repo")
        }
      }
      developers {
        developer {
          id.set("BoD")
          name.set("Benoit 'BoD' Lubek")
          email.set("BoD@JRAF.org")
          url.set("https://JRAF.org")
          organization.set("JRAF.org")
          organizationUrl.set("https://JRAF.org")
          roles.set(listOf("developer"))
          timezone.set("+1")
        }
      }
      scm {
        connection.set("scm:git:https://github.com/BoD/klibslack")
        developerConnection.set("scm:git:https://github.com/BoD/klibslack")
        url.set("https://github.com/BoD/klibslack")
      }
      issueManagement {
        url.set("https://github.com/BoD/klibslack/issues")
        system.set("GitHub Issues")
      }
    }
  }
}

signing {
  // Note: declare the signature key, password and file in your home's gradle.properties like this:
  // signing.keyId=<8 character key>
  // signing.password=<your password>
  // signing.secretKeyRingFile=<absolute path to the gpg private key>
  sign(publishing.publications)
}

// Workaround for https://youtrack.jetbrains.com/issue/KT-46466
val dependsOnTasks = mutableListOf<String>()
tasks.withType<AbstractPublishToMaven>().configureEach {
  dependsOnTasks.add(this.name.replace("publish", "sign").replaceAfter("Publication", ""))
  dependsOn(dependsOnTasks)
}

dokka {
  dokkaPublications.html {
    outputDirectory.set(rootProject.file("docs"))
  }
}

// Run `./gradlew dokkaHtml` to generate the docs
// Run `./gradlew publishToMavenLocal` to publish to the local maven repo
// Run `./gradlew publish` to publish to Maven Central (then go to https://oss.sonatype.org/#stagingRepositories and "close", and "release")
