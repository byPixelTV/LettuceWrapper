plugins {
    kotlin("jvm") version "2.2.0"
    id("maven-publish")
    id("signing")
}

group = "dev.bypixel"
version = "1.0.0"

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
                username = System.getenv("REPO_USERNAME")
                password = System.getenv("REPO_PASSWORD")
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