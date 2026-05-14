plugins {
    kotlin("jvm") version "2.3.21"
    id("maven-publish")
    kotlin("plugin.serialization") version "2.3.21"
}


fun getLatestTag(): String {
    try {
        // Fetch all tags
        ProcessBuilder("git", "fetch", "--tags")
            .redirectErrorStream(true)
            .start()
            .apply {
                inputStream.bufferedReader().use { it.readText() }
                waitFor()
            }

        val branch = ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD")
            .redirectErrorStream(true)
            .start()
            .inputStream
            .bufferedReader()
            .use { it.readText().trim() }

        // Try to get latest tag
        val tagProcess = ProcessBuilder("git", "describe", "--tags", "--abbrev=0")
            .redirectErrorStream(true)
            .start()
        val rawTag = tagProcess.inputStream.bufferedReader().use { it.readText().trim() }
        tagProcess.waitFor()

        val hasTag = rawTag.isNotEmpty() && !rawTag.startsWith("fatal:")

        // Always get commit hash (works even if no tag)
        val commitProcess = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
            .redirectErrorStream(true)
            .start()
        val commit = commitProcess.inputStream.bufferedReader().use { it.readText().trim() }
        commitProcess.waitFor()

        // If no commit found (super rare, empty repo)
        if (commit.isEmpty()) return "unknown"

        return if (hasTag) {
            val tag = rawTag.removePrefix("v")
            if (branch == "release") tag else "$tag+$commit"
        } else {
            // no tag → default to 1.0.0 + commit
            "1.0.0+$commit"
        }
    } catch (_: Exception) {
        return "unknown"
    }
}

val versionString = getLatestTag()

group = "com.github.bypixeltv"
version = versionString


repositories {
    mavenCentral()
}

dependencies {
    api("io.lettuce:lettuce-core:7.5.1.RELEASE")
    api("org.json:json:20250517")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.2")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = project.name
        }
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    jvmToolchain(21)
}