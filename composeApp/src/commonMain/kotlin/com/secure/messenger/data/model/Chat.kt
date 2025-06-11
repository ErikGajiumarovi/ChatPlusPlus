package com.secure.messenger.data.model

data class Chat(
    val id: String = "",
    val name: String? = null,
    val participantEmails: List<String> = emptyList(),
    val lastMessageContent: String? = null,
    val lastMessageTimestamp: Long = 0,
    val isGroupChat: Boolean = false
)
