plugins {
    kotlin("jvm") version "2.2.21"
    id("maven-publish")
    id("signing")
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
    } catch (e: Exception) {
        return "unknown"
    }
}

val versionString = getLatestTag()

group = "dev.bypixel"
version = versionString


repositories {
    mavenCentral()
}

dependencies {
    compileOnly("io.lettuce:lettuce-core:6.8.1.RELEASE") {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-reactive")
    }
    compileOnly("org.json:json:20250517")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.10.2")
}


publishing {
    repositories {
        maven {
            name = "bypixelReleases"
            url = uri("https://repo.bypixel.dev/releases/")
            credentials{
                username = findProperty("bypixelRepoUser").toString()
                password = findProperty("bypixelRepoToken").toString()
            }
        }

        maven {
            name = "bypixelSnapshots"
            url = uri("https://repo.bypixel.dev/snapshots/")
            credentials{
                username = findProperty("bypixelRepoUser").toString()
                password = findProperty("bypixelRepoToken").toString()
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = rootProject.group.toString()
            artifactId = project.name
            version = rootProject.version.toString()
            from(project.components["java"])
        }
    }
}

kotlin {
    jvmToolchain(21)
}