package dev.bypixel.lettucewrapper.stream

import io.lettuce.core.Limit
import io.lettuce.core.Range
import io.lettuce.core.StreamMessage
import io.lettuce.core.XReadArgs
import io.lettuce.core.XReadArgs.StreamOffset
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext

/** Delivers stream entries in order to one callback per invocation of [listen]. */
abstract class RedisStreamListener(private val stream: RedisStream) {
    /**
     * Listens until the calling coroutine is cancelled or an error occurs.
     * Pass `0-0` to replay existing entries, an entry ID to resume after it, or `$` to start at the current end.
     */
    suspend fun listen(afterId: String = "0-0", batchSize: Long = 100): Unit = withContext(Dispatchers.IO) {
        stream.client.redisClient.connect(StringCodec.UTF8).use { connection ->
            val commands = connection.async()
            var cursor = if (afterId == "$") {
                commands.xrevrange(stream.key, Range.unbounded(), Limit.from(1)).await().firstOrNull()?.id ?: "0-0"
            } else {
                afterId
            }
            val args = XReadArgs.Builder.block(1_000).count(batchSize)
            while (true) {
                ensureActive()
                val messages = commands.xread(args, StreamOffset.from(stream.key, cursor)).await().orEmpty()
                for (message in messages) {
                    ensureActive()
                    onMessage(message)
                    cursor = message.id
                }
            }
        }
    }

    abstract suspend fun onMessage(message: StreamMessage<String, String>)
}
