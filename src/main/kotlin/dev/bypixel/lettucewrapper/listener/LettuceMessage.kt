package dev.bypixel.lettucewrapper.listener

import kotlinx.serialization.Serializable

@Serializable
open class LettuceMessage(
    val action: String,
    val channel: String,
    val createdAt: Long = System.currentTimeMillis()
)