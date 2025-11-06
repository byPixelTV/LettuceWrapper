package dev.bypixel.lettucewrapper.listener

import dev.bypixel.lettucewrapper.LettuceRedisClient
import io.lettuce.core.pubsub.RedisPubSubListener
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

abstract class RedisListener(
    channels: Any = emptyList<String>(),
    private val listenToAll: Boolean = false
) {
    private val channels: List<String> = when (channels) {
        is String -> listOf(channels)
        is List<*> -> channels.filterIsInstance<String>()
        else -> emptyList()
    }

    init {
        registerListener(this)
    }

    fun unregister() = unregisterListener(this)

    companion object {
        private val listeners = mutableSetOf<RedisListener>()
        private var lettuceClient: LettuceRedisClient? = null
        private var isSubscribed = false

        private val listenerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        private lateinit var pubSubConnection: StatefulRedisPubSubConnection<String, String>
        private lateinit var pubSubCommands: RedisPubSubReactiveCommands<String, String>

        private fun subscribeToAllChannels() {
            if (lettuceClient == null)
                throw IllegalStateException("LettuceRedisClient not set. Use RedisListener.setLettuceClient() first.")

            if (!isSubscribed) {
                pubSubConnection = lettuceClient!!.redisClient.connectPubSub()
                pubSubCommands = pubSubConnection.reactive()

                pubSubConnection.addListener(object : RedisPubSubListener<String, String> {
                    override fun message(channel: String, message: String) {
                        listeners.forEach { listener ->
                            if (listener.listenToAll || listener.channels.contains(channel)) {
                                listenerScope.launch { listener.onMessage(channel, message) }
                            }
                        }
                    }

                    override fun message(pattern: String, channel: String, message: String) {
                        listeners.forEach { listener ->
                            if (listener.listenToAll || listener.channels.contains(channel)) {
                                listenerScope.launch { listener.onMessage(channel, message) }
                            }
                        }
                    }

                    override fun subscribed(channel: String?, count: Long) {}
                    override fun psubscribed(pattern: String?, count: Long) {}
                    override fun unsubscribed(channel: String?, count: Long) {}
                    override fun punsubscribed(pattern: String?, count: Long) {}
                })

                listenerScope.launch { pubSubCommands.psubscribe("*").subscribe() }
                isSubscribed = true
            }
        }

        private fun registerListener(listener: RedisListener) {
            listeners.add(listener)
            subscribeToAllChannels()
        }

        fun unregisterListener(listener: RedisListener) = listeners.remove(listener)
        fun setLettuceClient(client: LettuceRedisClient) { lettuceClient = client }
    }

    open fun onMessage(channel: String, message: String) {
        onMessage(message)
    }

    open fun onMessage(message: String) {}
}