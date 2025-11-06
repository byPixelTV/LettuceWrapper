package dev.bypixel.lettucewrapper

data class RedisCredentials(
    val host: String,
    val port: Int = 6379,
    val username: String? = null,
    val password: String? = null,
    val db: Int? = null,
    val ssl: Boolean = false,
    val allowSelfSignedInsecure: Boolean = false,
    val trustStorePath: String? = null,
    val trustStorePassword: String? = null
) {
    fun createUrl(): String {
        require(host.isNotBlank()) { "Host cannot be empty." }

        val scheme = if (ssl) "rediss" else "redis"
        val auth = when {
            username != null && password != null -> "$username:$password@"
            username != null -> "$username@"
            password != null -> ":$password@"
            else -> ""
        }

        val dbPart = db?.let { "/$it" } ?: ""
        return "$scheme://$auth$host:$port$dbPart"
    }

    fun parseUrl(url: String): RedisCredentials? {
        return try {
            val uri = java.net.URI(url)
            if (uri.scheme != "redis" && uri.scheme != "rediss") return null

            val ssl = uri.scheme == "rediss"
            val userInfo = uri.userInfo?.split(":", limit = 2)
            val username = userInfo?.getOrNull(0)?.takeIf { it.isNotEmpty() }
            val password = userInfo?.getOrNull(1)?.takeIf { it.isNotEmpty() }

            val host = uri.host ?: return null
            val port = if (uri.port != -1) uri.port else 6379
            val db = uri.path?.trimStart('/')?.takeIf { it.isNotBlank() }?.toIntOrNull()

            RedisCredentials(host, port, username, password, db, ssl)
        } catch (_: Exception) {
            null
        }
    }
}