package com.secure.messenger.data

import com.secure.messenger.data.model.Chat
import com.secure.messenger.data.model.Message
import com.secure.messenger.data.model.User
import kotlinx.coroutines.flow.Flow

// Определяем общий интерфейс для всех платформ
interface FirebaseClientInterface {
    // Authentication Methods
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signUp(email: String, password: String): Result<User>
    suspend fun signInAnonymously(): Result<User> // Новый метод для анонимной авторизации
    suspend fun signOut()
    suspend fun getCurrentUser(): User?

    // User Methods
    suspend fun getUserByEmail(email: String): User?

    // Message Methods
    suspend fun sendMessage(message: Message)
    fun observeMessages(chatId: String): Flow<List<Message>>

    // Chat Methods
    suspend fun createChat(participantEmails: List<String>, name: String? = null): String
    fun observeUserChats(userEmail: String): Flow<List<Chat>>
    suspend fun getChat(chatId: String): Chat?
}

// Используем expect/actual паттерн для платформенно-специфичной реализации
expect class FirebaseClient() : FirebaseClientInterface
