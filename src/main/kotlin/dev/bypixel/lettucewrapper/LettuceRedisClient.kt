package dev.bypixel.lettucewrapper

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.output.IntegerOutput
import io.lettuce.core.output.StatusOutput
import io.lettuce.core.output.ValueOutput
import io.lettuce.core.protocol.CommandArgs
import io.lettuce.core.protocol.CommandType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class LettuceRedisClient(host: String, port: Int, private val password: String?, private val coroutineScope: CoroutineScope) {

    private val redisUri = RedisURI.Builder.redis(host, port).apply {
        password?.takeIf { it.isNotBlank() }?.let { withPassword(it.toCharArray()) }
    }.build()

    val redisClient: RedisClient = RedisClient.create(redisUri)
    val connection: StatefulRedisConnection<String, String> = redisClient.connect()

    // Coroutine Commands
    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    val commands = connection.coroutines()

    // sync commands
    val sync: RedisCommands<String, String> = connection.sync()

    suspend fun jsonSet(key: String, path: String = ".", json: String): String? = withContext(Dispatchers.IO) {
        connection.async().dispatch(
            CommandType.valueOf("JSON.SET"),
            StatusOutput(StringCodec.UTF8),
            CommandArgs(StringCodec.UTF8).add(key).add(path).add(json)
        ).await()
    }

    suspend fun jsonGet(key: String, path: String = "."): String? = withContext(Dispatchers.IO) {
        connection.async().dispatch(
            CommandType.valueOf("JSON.GET"),
            ValueOutput(StringCodec.UTF8),
            CommandArgs(StringCodec.UTF8).add(key).add(path)
        ).await()
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    fun sendMessage(message: JSONObject, channel: String) {
        coroutineScope.launch(Dispatchers.IO) {
            connection.coroutines().publish(channel, message.toString())
        }
    }

    suspend fun jsonDel(key: String, path: String = "."): Long = withContext(Dispatchers.IO) {
        connection.async().dispatch(
            CommandType.valueOf("JSON.DEL"),
            IntegerOutput(StringCodec.UTF8),
            CommandArgs(StringCodec.UTF8).add(key).add(path)
        ).await()
    }

    fun jsonSetSync(key: String, path: String = ".", json: String): String? {
        return connection.sync().dispatch(
            CommandType.valueOf("JSON.SET"),
            StatusOutput(StringCodec.UTF8),
            CommandArgs(StringCodec.UTF8).add(key).add(path).add(json)
        )
    }

    fun jsonGetSync(key: String, path: String = "."): String? {
        return connection.sync().dispatch(
            CommandType.valueOf("JSON.GET"),
            ValueOutput(StringCodec.UTF8),
            CommandArgs(StringCodec.UTF8).add(key).add(path)
        )
    }

    fun jsonDelSync(key: String, path: String = "."): Long {
        return connection.sync().dispatch(
            CommandType.valueOf("JSON.DEL"),
            IntegerOutput(StringCodec.UTF8),
            CommandArgs(StringCodec.UTF8).add(key).add(path)
        ) ?: 0
    }

    fun returnKeysWithMatchingValue(key: String, value: String): List<String> {
        return connection.sync().hkeys(key)
            .filter { field -> connection.sync().hget(key, field) == value }
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    suspend fun returnKeysWithMatchingValueAsync(key: String, value: String): List<String> {
        return connection.coroutines().hkeys(key)
            .filter { field -> connection.async().hget(key, field).await() == value }
            .toList()
    }

    fun deleteHashFieldByValue(key: String, value: String): Long {
        val fieldsToDelete = returnKeysWithMatchingValue(key, value)
        return if (fieldsToDelete.isNotEmpty()) {
            connection.sync().hdel(key, *fieldsToDelete.toTypedArray())
        } else {
            0L
        }
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    suspend fun deleteHashFieldByValueAsync(key: String, value: String): Long {
        val fieldsToDelete = returnKeysWithMatchingValueAsync(key, value)
        return if (fieldsToDelete.isNotEmpty()) {
            connection.coroutines().hdel(key, *fieldsToDelete.toTypedArray()) ?: 0L
        } else {
            0L
        }
    }

    fun findKeysWithMatchingValuesAsList(key: String): List<String> {
        return connection.sync().hvals(key)
            .filterNotNull()
            .distinct()
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    suspend fun findKeysWithMatchingValuesAsListAsync(key: String): List<String> {
        return connection.coroutines().hvals(key)
            .filterNotNull()
            .toList()
            .distinct()
    }

    fun setTtlOfHashField(key: String, field: String, seconds: Long) {
        connection.sync().hexpire(key, seconds, field)
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    suspend fun setTtlOfHashFieldAsync(key: String, field: String, seconds: Long) {
        connection.coroutines().hexpire(key, seconds, field)
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        connection.close()
        redisClient.shutdown()
    }
}