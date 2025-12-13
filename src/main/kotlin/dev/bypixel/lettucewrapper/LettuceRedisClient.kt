package dev.bypixel.lettucewrapper

import dev.bypixel.lettucewrapper.listener.LettuceMessage
import io.lettuce.core.*
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.output.IntegerOutput
import io.lettuce.core.output.StatusOutput
import io.lettuce.core.output.ValueOutput
import io.lettuce.core.protocol.CommandArgs
import io.lettuce.core.protocol.CommandType
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.TrustManagerFactory
import kotlin.math.max

class LettuceRedisClient(
    credentials: RedisCredentials,
    val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    poolSize: Int = max(4, Runtime.getRuntime().availableProcessors() * 2)
) {

    private val redisUri = RedisURI.Builder.redis(credentials.host, credentials.port).apply {
        if (credentials.ssl) {
            withSsl(true)
            if (credentials.allowSelfSignedInsecure) {
                withVerifyPeer(false)
            }
        }

        credentials.username?.takeIf { it.isNotBlank() }?.let { withAuthentication(it, credentials.password?.toCharArray()) }
            ?: credentials.password?.takeIf { it.isNotBlank() }?.let { withPassword(it.toCharArray()) }

        credentials.db?.takeIf { it >= 0 }?.let { withDatabase(it) }
    }.build()

    val redisClient: RedisClient = RedisClient.create(redisUri).apply {
        if (credentials.ssl) {
            val sslOptions = when {
                !credentials.trustStorePath.isNullOrBlank() -> {
                    val ksType = if (credentials.trustStorePath.endsWith(".p12", ignoreCase = true) || credentials.trustStorePath.endsWith(".pfx", ignoreCase = true)) "PKCS12" else "JKS"
                    val ks = KeyStore.getInstance(ksType)
                    FileInputStream(credentials.trustStorePath).use { fis ->
                        ks.load(fis, credentials.trustStorePassword?.toCharArray())
                    }
                    val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                    tmf.init(ks)
                    SslOptions.builder().jdkSslProvider().trustManager(tmf).build()
                }
                credentials.allowSelfSignedInsecure -> {
                    SslOptions.builder().jdkSslProvider().trustManager(InsecureTrustManagerFactory.INSTANCE).build()
                }
                else -> {
                    SslOptions.builder().jdkSslProvider().build()
                }
            }
            val clientOptions = ClientOptions.builder()
                .sslOptions(sslOptions)
                .maintNotificationsConfig(MaintNotificationsConfig.builder().enableMaintNotifications(false).build())
                .build()
            options = clientOptions
        } else {
            val clientOptions = ClientOptions.builder()
                .maintNotificationsConfig(MaintNotificationsConfig.builder().enableMaintNotifications(false).build())
                .build()
            options = clientOptions
        }
    }

    private val connectionPool = Array(poolSize) { redisClient.connect() }
    private val semaphore = Semaphore(poolSize)

    val connection: StatefulRedisConnection<String, String> = redisClient.connect()

    suspend fun <T> withConnection(block: suspend (StatefulRedisConnection<String, String>) -> T): T {
        return semaphore.withPermit {
            val conn = connectionPool.random()
            block(conn)
        }
    }

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

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    inline fun <reified T : LettuceMessage> sendLettuceMessage(data: T) {
        coroutineScope.launch(Dispatchers.IO) {
            connection.coroutines().publish(
                data.channel,
                Json.encodeToString(data)
            )
        }
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    fun sendPlainMessage(message: String, channel: String) {
        coroutineScope.launch(Dispatchers.IO) {
            connection.coroutines().publish(channel, message)
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
        if (connection.isOpen) {
            connection.close()
        }
        redisClient.shutdown()
        connectionPool.forEach {
            if (it.isOpen) {
                it.close()
            }
        }
    }
}