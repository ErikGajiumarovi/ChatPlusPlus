package com.secure.messenger.data.model

data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isEncrypted: Boolean = false,
    val encryptionAlgorithm: String? = null
)
