<div>
<h1 align="center">LettuceWrapper 🥬</h1>
</div>

<div align="center">

![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/byPixelTV/LettuceWrapper/build_release.yml?branch=release&style=for-the-badge&label=Release%20Build&logo=github)
![GitHub Actions Workflow Status Snapshots](https://img.shields.io/github/actions/workflow/status/byPixelTV/LettuceWrapper/build_release.yml?branch=nightly&style=for-the-badge&label=Snapshot%20Build&logo=github)
![GitHub issues](https://img.shields.io/github/issues-raw/byPixelTV/lettucewrapper?style=for-the-badge)
<br>
[![CodeFactor](https://www.codefactor.io/repository/github/bypixeltv/lettucewrapper/badge)](https://www.codefactor.io/repository/github/bypixeltv/lettucewrapper)
![](https://sloc.xyz/github/byPixelTV/LettuceWrapper)

</div>

<h3 align="center">A modern coroutine-based wrapper for Lettuce</h3>
<hr>

LettuceWrapper provides coroutine helpers for Redis commands, Pub/Sub listeners, and Redis Streams. Use **Java 21 or newer** and a Redis server that supports the commands your application uses.

<div align="center">
  <a href="https://discord.gg/yVp7Qvhj9k" target="_blank">
    <img src="https://cdn.bypixel.dev/raw/mXHMir.png" height="64" alt="discord logo" />
  </a>
</div>
<hr>

<div align="center">
    <h3 align="center">Uses modern technology 🚀</h3>
  <img src="https://cdn.bypixel.dev/raw/QhWGzB.png" height="64" alt="kotlin" />
    <img src="https://cdn.bypixel.dev/raw/rptkK4.png" height="64" alt="gradle" />
</div>
<hr>

## Installation

[![Latest Release](https://repo.bypixel.dev/api/badge/latest/releases/dev/bypixel/LettuceWrapper?color=40c14a&name=LettuceWrapper-Release)](https://repo.bypixel.dev/#/releases/dev/bypixel/LettuceWrapper)
[![Latest Snapshot](https://repo.bypixel.dev/api/badge/latest/snapshots/dev/bypixel/LettuceWrapper?color=40c14a&name=LettuceWrapper-Snapshot)](https://repo.bypixel.dev/#/snapshots/dev/bypixel/LettuceWrapper)

Add these entries to your Kotlin project's `build.gradle.kts`. Replace `VERSION` with the LettuceWrapper version published in the repository you choose.

```kotlin
repositories {
    mavenCentral()
    maven("https://repo.bypixel.dev/releases")
    // For a nightly build, use https://repo.bypixel.dev/snapshots instead.
}

dependencies {
    implementation("dev.bypixel:LettuceWrapper:VERSION")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
}
```

Lettuce and `org.json` are transitive API dependencies. The wrapper's coroutine and serialization libraries are runtime dependencies. Declare `kotlinx-coroutines-core` directly when your code imports coroutine APIs such as `runBlocking` or `launch`, as the example below does.

Create a `LettuceRedisClient` with `RedisCredentials`. Use `withAsync`, `withCoroutines`, or `withSync` to call Lettuce commands, and call `client.close()` when the application is done. For Pub/Sub, call `RedisListener.setLettuceClient(client)` before creating a `RedisListener`.

## Redis Streams

Use a LettuceWrapper build that includes `client.stream(key)`. The example connects to Redis on `127.0.0.1:6379` by default. Set `REDIS_HOST` or `REDIS_PORT` to use a different address. Save the code as `StreamsExample.kt` and run `main` in your Kotlin/JVM project. Each run creates a new Redis key and leaves its entries available for inspection.

```kotlin
import dev.bypixel.lettucewrapper.LettuceRedisClient
import dev.bypixel.lettucewrapper.RedisCredentials
import dev.bypixel.lettucewrapper.stream.RedisStreamListener
import io.lettuce.core.StreamMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.UUID

fun main() = runBlocking {
    val client = LettuceRedisClient(
        RedisCredentials(
            host = System.getenv("REDIS_HOST") ?: "127.0.0.1",
            port = System.getenv("REDIS_PORT")?.toInt() ?: 6379
        )
    )

    try {
        val orders = client.stream("orders:example:${UUID.randomUUID()}")
        val group = "workers"
        orders.createGroup(group)

        val id = orders.append(mapOf("orderId" to "42", "status" to "created"))
        println("Appended $id to ${orders.key}")

        val entry = orders.read(count = 1).single()
        println("Read ${entry.id} with fields ${entry.body}")

        val delivered = CompletableDeferred<Unit>()
        val listener = object : RedisStreamListener(orders) {
            override suspend fun onMessage(message: StreamMessage<String, String>) {
                println("Listener received ${message.id} with fields ${message.body}")
                if (message.id == id) delivered.complete(Unit)
            }
        }

        val listenerJob = launch { listener.listen() }
        try {
            withTimeout(5_000) { delivered.await() }
        } finally {
            listenerJob.cancelAndJoin()
        }

        for (message in orders.readGroup(group, consumer = "worker-1")) {
            println("Worker processed ${message.id} with fields ${message.body}")
            val acknowledged = orders.acknowledge(group, message.id)
            println("Acknowledged $acknowledged entry")
        }
    } finally {
        client.close()
    }
}
```

`read()` and `listen()` start after `0-0` by default. Pass the last processed ID as `afterId` to resume after that entry. For `listen()`, use `afterId = "$"` to start at the current end. Each listener uses a dedicated connection and delivers entries sequentially until its coroutine is cancelled or an error occurs.

Consumer groups maintain their own delivery state. `createGroup()` returns `false` if the group already exists. `readGroup()` reads entries that are new to the group by default. Pass `afterId = "0-0"` to read pending entries for the same consumer. Call `acknowledge()` after successfully processing the entry. [Redis consumer group behavior](https://redis.io/docs/latest/commands/xreadgroup/)
