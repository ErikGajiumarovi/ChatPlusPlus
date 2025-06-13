package data

import data.model.Chat
import data.model.Message
import data.model.User
import kotlinx.coroutines.flow.Flow

actual class FirebaseClient actual constructor() {
    actual suspend fun signIn(email: String, password: String): Result<User> {
        TODO("Not yet implemented")
    }

    actual suspend fun signUp(email: String, password: String): Result<User> {
        TODO("Not yet implemented")
    }

    actual suspend fun signInAnonymously(): Result<User> {
        TODO("Not yet implemented")
    }

    actual suspend fun signOut() {
    }

    actual suspend fun getCurrentUser(): User? {
        TODO("Not yet implemented")
    }

    actual suspend fun getUserByEmail(email: String): User? {
        TODO("Not yet implemented")
    }

    actual suspend fun sendMessage(message: Message) {
    }

    actual fun observeMessages(chatId: String): Flow<List<Message>> {
        TODO("Not yet implemented")
    }

    actual suspend fun createChat(
        participantEmails: List<String>,
        name: String?
    ): String {
        TODO("Not yet implemented")
    }

    actual fun observeUserChats(userEmail: String): Flow<List<Chat>> {
        TODO("Not yet implemented")
    }

    actual suspend fun getChat(chatId: String): Chat? {
        TODO("Not yet implemented")
    }

    actual suspend fun markChatAsRead(chatId: String, userEmail: String) {
    }

    actual suspend fun updateUnreadCount(chatId: String, senderEmail: String) {
    }
}