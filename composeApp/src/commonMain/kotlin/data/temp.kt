package data

import data.model.Chat
import data.model.Message
import data.model.User
import kotlinx.coroutines.flow.Flow

expect class FirebaseClient()  {
     suspend fun signIn(
        email: String,
        password: String
    ): Result<User>

     suspend fun signUp(
        email: String,
        password: String
    ): Result<User>

     suspend fun signInAnonymously(): Result<User>

     suspend fun signOut()
     suspend fun getCurrentUser(): User?
     suspend fun getUserByEmail(email: String): User?
     suspend fun sendMessage(message: Message)
     fun observeMessages(chatId: String): Flow<List<Message>>
     suspend fun createChat(
        participantEmails: List<String>,
        name: String?
    ): String

     fun observeUserChats(userEmail: String): Flow<List<Chat>>
     suspend fun getChat(chatId: String): Chat?

     suspend fun markChatAsRead(chatId: String, userEmail: String)
     suspend fun updateUnreadCount(chatId: String, senderEmail: String)
}