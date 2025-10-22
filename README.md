<div>
<h1 align="center">LettuceWrapper 🥬</h1>
</div>

<div align="center">

![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/byPixelTV/LettuceWrapper/build.yml?branch=release&style=for-the-badge)
![GitHub issues](https://img.shields.io/github/issues-raw/byPixelTV/lettucewrapper?style=for-the-badge)
<br>
[![CodeFactor](https://www.codefactor.io/repository/github/bypixeltv/lettucewrapper/badge)](https://www.codefactor.io/repository/github/bypixeltv/lettucewrapper)
![](https://sloc.xyz/github/byPixelTV/LettuceWrapper)

</div>

<h3 align="center">A modern coroutine-based wrapper for Lettuce</h3>
<hr>

<div>

>
> # ⛏️ Software Support & Requirements 🎮:
> - Lettuce wrapper does not provide Lettuce, you have to add it by yourself.
> - LettuceWrapper requires the org.json JSON library, kotlinx-coroutines-core and kotlinx-coroutines-reactive to work properly. 
> - LettuceWrapper requires **Java 21** to run, you can find the latest version of Java [here](https://adoptium.net/).
> - LettuceWrapper requires the latest version of [Lettuce](https://lettuce.io/) to run.
</div>

<div align="center">
  <a href="https://discord.gg/yVp7Qvhj9k" target="_blank">
    <img src="https://cdn.bypixel.dev/raw/mXHMir.png" height="64" alt="discord logo" />
  </a>
</div>
<hr>

<div>

> # 💻 Development Builds 🌐:
> - **Development  Builds:** https://github.com/byPixelTV/LettuceWrapper/actions
</div>

<div align="center">
    <h3 align="center">Uses modern technology 🚀</h3>
  <img src="https://cdn.bypixel.dev/raw/QhWGzB.png" height="64" alt="kotlin" />
    <img src="https://cdn.bypixel.dev/raw/rptkK4.png" height="64" alt="gradle" />
</div>
<hr>

<div>

> # 🔥 Use LettuceWrapper 🚀
>
> 1. Add one of the Maven repositories to your build file:
>
> Release:
> ```kotlin
> maven {
>     name = "bypixelRepoReleases"
>     url = uri("https://repo.bypixel.dev/releases")
> }
> ```
> Snapshot:
> ```kotlin
> maven {
>     name = "bypixelRepoSnapshots"
>     url = uri("https://repo.bypixel.dev/snapshots")
> }
> ```
> 2. Add the dependency and Lettuce to your build file:
>
> LettuceWrapper: 
> 
> [![Latest Version](https://repo.bypixel.dev/api/badge/latest/releases/dev/bypixel/LettuceWrapper?color=40c14a&name=LettuceWrapper-Release&filter=none:+)](https://repo.bypixel.dev/#/releases/dev/bypixel/LettuceWrapper)
> 
> [![Snapshot Version](https://repo.bypixel.dev/api/badge/latest/releases/dev/bypixel/LettuceWrapper?color=40c14a&name=LettuceWrapper-Snapshot&filter=has:+)](https://repo.bypixel.dev/#/releases/dev/bypixel/LettuceWrapper)
> 
> Lettuce:
> 
> [![Maven Central](https://img.shields.io/maven-central/v/io.lettuce/lettuce-core?versionSuffix=RELEASE&logo=redis
)](https://maven-badges.herokuapp.com/maven-central/io.lettuce/lettuce-core)
> 
> JSON
>
> [![Maven Central](https://img.shields.io/maven-central/v/org.json/json.svg?logo=json)](https://mvnrepository.com/artifact/org.json/json)
> 
> KotlinX:
>
> [![Maven Central](https://img.shields.io/maven-central/v/org.jetbrains.kotlinx/kotlinx-coroutines-core?versionPrefix=1&logo=kotlin
)](https://maven-badges.herokuapp.com/maven-central/io.lettuce/lettuce-core)
> ```kotlin
> dependencies {
>    implementation("dev.bypixel:LettuceWrapper:VERSION")
>    implementation("io.lettuce:lettuce-core:VERSION")
>    implementation("org.json:json:VERSION")
>    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:VERSION")
>    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:VERSION")
> }
> ```
> Replace `VERSION` with the latest version of LettuceWrapper and Lettuce.
> 3. Start using LettuceWrapper in your project! For more information, check the [docs](https://docs.bypixel.dev/lettucewrapper).

</div>