package com.secure.messenger.data

import com.secure.messenger.data.model.Chat
import com.secure.messenger.data.model.Message
import com.secure.messenger.data.model.User
import com.secure.messenger.util.TimeUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * iOS реализация FirebaseClient
 *
 * Примечание: Это упрощенная реализация для запуска приложения на iOS.
 * Для полноценной работы с Firebase на iOS понадобится интеграция с нативными библиотеками.
 */
actual class FirebaseClient : FirebaseClientInterface {

    // Временное хранилище данных для локальной работы
    private val currentUserFlow = MutableStateFlow<User?>(null)
    private val messages = mutableListOf<Message>()
    private val chats = mutableListOf<Chat>()
    private val users = mutableListOf<User>() // Локальное хранилище пользователей для тестирования

    actual override suspend fun signIn(
        email: String,
        password: String
    ): Result<User> {
        // Для тестирования создаем фиктивного пользователя
        val user = User(
            id = "ios-user-1",
            email = email,
            displayName = email.substringBefore('@')
        )
        currentUserFlow.value = user

        // Добавляем пользователя в локальное хранилище если его еще нет
        if (users.none { it.email == email }) {
            users.add(user)
        }

        return Result.success(user)
    }

    actual override suspend fun signUp(
        email: String,
        password: String
    ): Result<User> {
        // Для тестирования просто возвращаем нового пользователя
        val user = User(
            id = "ios-user-" + TimeUtil.currentTimeMillis(),
            email = email,
            displayName = email.substringBefore('@')
        )
        currentUserFlow.value = user

        // Добавляем пользователя в локальное хранилище
        users.add(user)

        return Result.success(user)
    }

    actual override suspend fun signInAnonymously(): Result<User> {
        // Создаем анонимного пользователя
        val id = "anon-" + TimeUtil.currentTimeMillis()
        val user = User(
            id = id,
            email = "",
            displayName = "Гость ${id.takeLast(5)}"
        )
        currentUserFlow.value = user

        // Добавляем пользователя в локальное хранилище
        users.add(user)

        return Result.success(user)
    }

    actual override suspend fun signOut() {
        currentUserFlow.value = null
    }

    actual override suspend fun getCurrentUser(): User? {
        return currentUserFlow.value
    }

    actual override suspend fun getUserByEmail(email: String): User? {
        return users.find { it.email == email }
    }

    actual override suspend fun sendMessage(message: Message) {
        messages.add(message)

        // Обновляем последнее сообщение в чате
        val chatIndex = chats.indexOfFirst { it.id == message.chatId }
        if (chatIndex >= 0) {
            val chat = chats[chatIndex]
            chats[chatIndex] = chat.copy(
                lastMessageContent = message.content,
                lastMessageTimestamp = message.timestamp
            )
        }
    }

    actual override fun observeMessages(chatId: String): Flow<List<Message>> {
        return flowOf(messages.filter { it.chatId == chatId })
    }

    actual override suspend fun createChat(participantEmails: List<String>, name: String?): String {
        val id = "chat-" + TimeUtil.currentTimeMillis()
        val chat = Chat(
            id = id,
            participantEmails = participantEmails,
            name = name,
            isGroupChat = participantEmails.size > 2
        )
        chats.add(chat)
        return id
    }

    actual override fun observeUserChats(userEmail: String): Flow<List<Chat>> {
        return flowOf(chats.filter { it.participantEmails.contains(userEmail) })
    }

    actual override suspend fun getChat(chatId: String): Chat? {
        return chats.find { it.id == chatId }
    }

    // Unread Messages Methods
    actual override suspend fun markChatAsRead(chatId: String, userEmail: String) {
        try {
            // Находим чат в локальном хранилище
            val chatIndex = chats.indexOfFirst { it.id == chatId }
            if (chatIndex >= 0) {
                val chat = chats[chatIndex]

                // Обновляем timestamp последнего прочитанного сообщения для пользователя
                val lastReadMap = chat.lastReadTimestamp.toMutableMap()
                lastReadMap[userEmail] = System.currentTimeMillis()

                // Заменяем чат обновленной версией со сброшенным счетчиком непрочитанных сообщений
                chats[chatIndex] = chat.copy(
                    lastReadTimestamp = lastReadMap,
                    unreadCount = 0
                )
            }
        } catch (e: Exception) {
            println("Error marking chat as read: ${e.message}")
        }
    }

    actual override suspend fun updateUnreadCount(chatId: String, senderEmail: String) {
        try {
            // Находим чат в локальном хранилище
            val chatIndex = chats.indexOfFirst { it.id == chatId }
            if (chatIndex >= 0) {
                val chat = chats[chatIndex]

                // Увеличиваем счетчик непрочитанных сообщений
                chats[chatIndex] = chat.copy(
                    unreadCount = chat.unreadCount + 1
                )
            }
        } catch (e: Exception) {
            println("Error updating unread count: ${e.message}")
        }
    }
}