package data.model

import kotlinx.serialization.Serializable

@Serializable
data class Chat(
    val id: String = "",
    val name: String? = null,
    val participantEmails: List<String> = emptyList(),
    val lastMessageContent: String? = null,
    val lastMessageTimestamp: Long = 0,
    val isGroupChat: Boolean = false,
    val unreadCount: Int = 0,
    val lastReadTimestamp: Map<String, Long> = emptyMap(),
    val unreadMessagesByUser: Map<String, Int> = emptyMap()
)
