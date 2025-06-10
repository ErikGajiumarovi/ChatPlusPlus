package com.secure.messenger.data

// Оставляем только определения модели и импорты, которые будут работать на всех платформах
import com.secure.messenger.data.model.Chat
import com.secure.messenger.data.model.Message
import com.secure.messenger.data.model.User
import kotlinx.coroutines.flow.Flow

// Вся реализация будет в платформенно-специфичных модулях (androidMain, iosMain)
expect class FirebaseClient() : FirebaseClientInterface {
    override suspend fun signIn(
        username: String,
        password: String
    ): Result<User>

    override suspend fun signUp(
        username: String,
        password: String
    ): Result<User>

    override suspend fun signInAnonymously(): Result<User>

    override suspend fun signOut()
    override suspend fun getCurrentUser(): User?
    override suspend fun sendMessage(message: Message)
    override fun observeMessages(chatId: String): Flow<List<Message>>
    override suspend fun createChat(
        participantIds: List<String>,
        name: String?
    ): String

    override fun observeUserChats(userId: String): Flow<List<Chat>>
    override suspend fun getChat(chatId: String): Chat?
}
