package data.repository

import data.FirebaseClient
import data.NewFirebaseClient
import data.model.Message
import data.model.User
import data.model.Chat
import kotlinx.coroutines.flow.Flow

class MessagesRepository(private val firebaseClient: NewFirebaseClient) {

    suspend fun sendMessage(chatId: String, content: String): Result<Unit> {
        return try {
            val currentUser = firebaseClient.getCurrentUser() ?:
                return Result.failure(Exception("User not authenticated"))

            val message = Message(
                chatId = chatId,
                senderEmail = currentUser.email,
                content = content
            )

            firebaseClient.sendMessage(message)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeMessages(chatId: String): Flow<List<Message>> {
        return firebaseClient.observeMessages(chatId)
    }

    suspend fun createChat(participantEmails: List<String>, chatName: String? = null): Result<String> {
        return try {
            val chatId = firebaseClient.createChat(participantEmails, chatName)
            Result.success(chatId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeUserChats(userEmail: String): Flow<List<Chat>> {
        return firebaseClient.observeUserChats(userEmail)
    }

    suspend fun getUserByEmail(email: String): User? {
        return firebaseClient.getUserByEmail(email)
    }

    suspend fun getChat(chatId: String): Chat? {
        return firebaseClient.getChat(chatId)
    }

    suspend fun markChatAsRead(chatId: String): Result<Unit> {
        return try {
            val currentUser = firebaseClient.getCurrentUser() ?:
                return Result.failure(Exception("User not authenticated"))

            firebaseClient.markChatAsRead(chatId, currentUser.email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
