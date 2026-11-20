package dev.bypixel.lettucewrapper.stream

import dev.bypixel.lettucewrapper.LettuceRedisClient
import io.lettuce.core.Consumer
import io.lettuce.core.RedisCommandExecutionException
import io.lettuce.core.StreamMessage
import io.lettuce.core.XGroupCreateArgs
import io.lettuce.core.XReadArgs
import io.lettuce.core.XReadArgs.StreamOffset
import kotlinx.coroutines.future.await

/** Field/value entries stored under one Redis Stream key. */
class RedisStream internal constructor(
    internal val client: LettuceRedisClient,
    val key: String
) {
    /** Appends an entry and returns the ID assigned by Redis. */
    suspend fun append(fields: Map<String, String>): String =
        client.withAsync { it.xadd(key, fields).await() }

    /** Reads up to [count] entries with IDs strictly greater than [afterId]. */
    suspend fun read(afterId: String = "0-0", count: Long = 100): List<StreamMessage<String, String>> =
        client.withAsync { it.xread(XReadArgs.Builder.count(count), StreamOffset.from(key, afterId)).await().orEmpty() }

    /** Creates the stream if needed. Returns false if the group already exists. */
    suspend fun createGroup(group: String, afterId: String = "0-0"): Boolean = client.withAsync {
        try {
            it.xgroupCreate(StreamOffset.from(key, afterId), group, XGroupCreateArgs.Builder.mkstream()).await()
            true
        } catch (error: RedisCommandExecutionException) {
            if (error.message?.startsWith("BUSYGROUP ") != true) throw error
            false
        }
    }

    /** Reads new entries with `>`, or this consumer's pending entries after an explicit ID. */
    suspend fun readGroup(
        group: String,
        consumer: String,
        afterId: String = ">",
        count: Long = 100
    ): List<StreamMessage<String, String>> = client.withAsync {
        it.xreadgroup(Consumer.from(group, consumer), XReadArgs.Builder.count(count), StreamOffset.from(key, afterId))
            .await().orEmpty()
    }

    /** Returns the number of pending entries acknowledged by Redis. */
    suspend fun acknowledge(group: String, vararg ids: String): Long {
        if (ids.isEmpty()) return 0
        return client.withAsync { it.xack(key, group, *ids).await() }
    }
}
