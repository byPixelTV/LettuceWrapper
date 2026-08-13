import java.io.ByteArrayOutputStream

plugins {
    kotlin("jvm") version "2.4.10"
    id("maven-publish")
    kotlin("plugin.serialization") version "2.3.21"
}

fun runGit(vararg args: String): String? {
    return try {
        val output = ByteArrayOutputStream()
        val process = Runtime.getRuntime().exec(arrayOf("git", *args))
        val exit = process.waitFor()
        if (exit == 0) {
            process.inputStream.copyTo(output)
            output.toString().trim().ifEmpty { null }
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}

fun getGitBranch(): String {
    return try {
        val output = ByteArrayOutputStream()
        Runtime.getRuntime().exec(arrayOf("git", "rev-parse", "--abbrev-ref", "HEAD")).apply {
            waitFor()
            inputStream.copyTo(output)
        }
        output.toString().trim()
    } catch (_: Exception) {
        "unknown-branch"
    }
}

fun getCurrentGitCommit(): String {
    return try {
        val output = ByteArrayOutputStream()
        Runtime.getRuntime().exec(arrayOf("git", "rev-parse", "--short", "HEAD")).apply {
            waitFor()
            inputStream.copyTo(output)
        }
        output.toString().trim()
    } catch (_: Exception) {
        "unknown-commit"
    }
}

fun latestReleaseTagOrDefault(defaultTag: String = "v0.0.0"): String {
    val releaseTagPattern = Regex("""^v\d+\.\d+\.\d+$""")
    val tags = runGit("tag", "--list", "v[0-9]*.[0-9]*.[0-9]*", "--sort=-v:refname")
        ?.lineSequence()
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: emptySequence()

    return tags.firstOrNull { releaseTagPattern.matches(it) } ?: defaultTag
}

fun parseReleaseTag(tag: String): Triple<Int, Int, Int>? {
    val match = Regex("""^v(\d+)\.(\d+)\.(\d+)$""").matchEntire(tag) ?: return null
    return Triple(
        match.groupValues[1].toInt(),
        match.groupValues[2].toInt(),
        match.groupValues[3].toInt()
    )
}

fun snapshotBuildId(): String {
    val fromProperty = findProperty("snapshotBuild")?.toString()?.trim().orEmpty()
    if (fromProperty.isNotEmpty()) return fromProperty

    val fromEnv = System.getenv("GITHUB_RUN_NUMBER")?.trim().orEmpty()
    if (fromEnv.isNotEmpty()) return fromEnv

    return "local"
}

fun isFullReleaseBuild(): Boolean {
    val releaseProp = (findProperty("release")?.toString() ?: "false").toBooleanStrictOrNull() ?: false
    if (releaseProp) return true

    val branch = runGit("rev-parse", "--abbrev-ref", "HEAD") ?: ""
    return branch == "release"
}

fun computeVersion(): String {
    val releaseTag = latestReleaseTagOrDefault("v0.0.0")
    val (major, minor, patch) = parseReleaseTag(releaseTag) ?: Triple(1, 0, 0)
    val releaseVersion = "$major.$minor.$patch"

    if (isFullReleaseBuild()) {
        return releaseVersion
    }

    val nextPatchVersion = "$major.$minor.${patch + 1}"
    return "$nextPatchVersion-dev.${snapshotBuildId()}"
}

group = "dev.bypixel"
val calculatedVersion = computeVersion()
version = calculatedVersion
rootProject.version = calculatedVersion


repositories {
    mavenCentral()
}

dependencies {
    api("io.lettuce:lettuce-core:7.6.0.RELEASE")
    api("org.json:json:20250517")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
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

java {
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    jvmToolchain(21)
}