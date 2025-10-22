plugins {
    kotlin("jvm") version "2.2.0"
    id("maven-publish")
    id("signing")
}


fun getLatestTag(): String {
    try {
        // fetch all tags (remote + local)
        ProcessBuilder("git", "fetch", "--tags")
            .redirectErrorStream(true)
            .start()
            .apply {
                inputStream.bufferedReader().use { it.readText() }
                waitFor()
            }

        // get current branch
        val branch = ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD")
            .redirectErrorStream(true)
            .start()
            .inputStream
            .bufferedReader()
            .use { it.readText().trim() }

        // get latest tag
        val tagProcess = ProcessBuilder("git", "describe", "--tags", "--abbrev=0")
            .redirectErrorStream(true)
            .start()

        val rawTag = tagProcess.inputStream.bufferedReader().use { it.readText().trim() }
        tagProcess.waitFor()

        if (rawTag.isEmpty()) return "unknown"

        val tag = rawTag.removePrefix("v")

        return if (branch == "release") {
            tag
        } else {
            // get short commit hash
            val commitProcess = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
                .redirectErrorStream(true)
                .start()
            val commit = commitProcess.inputStream.bufferedReader().use { it.readText().trim() }
            commitProcess.waitFor()

            "$tag+$commit"
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
    implementation("io.lettuce:lettuce-core:6.8.1.RELEASE") {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-reactive")
    }
    implementation("org.json:json:20250517")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.10.2")
}


publishing {
    repositories {
        maven {
            name = "bypixelReleases"
            url = uri("https://repo.bypixel.dev/releases/")
            credentials{
                username = System.getenv("REPO_USERNAME")
                password = System.getenv("REPO_PASSWORD")
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