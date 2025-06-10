package com.secure.messenger.data.model

import com.secure.messenger.util.TimeUtil

data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val content: String = "",
    val timestamp: Long = TimeUtil.currentTimeMillis(),
    val isEncrypted: Boolean = false,
    val encryptionAlgorithm: String? = null
)
