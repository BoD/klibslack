plugins {
    // See https://jmfayard.github.io/refreshVersions
    id("de.fayard.refreshVersions") version "0.50.2"
}

rootProject.name = "klibslack-root"

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
