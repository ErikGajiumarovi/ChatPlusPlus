package data.model

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderEmail: String = "",
    val content: String = "",
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val isEncrypted: Boolean = false,
    val encryptionAlgorithm: String? = null
)
