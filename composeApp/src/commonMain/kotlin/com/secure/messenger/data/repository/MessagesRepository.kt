package com.secure.messenger.data.repository

import com.secure.messenger.data.FirebaseClient
import com.secure.messenger.data.model.Message
import com.secure.messenger.data.model.User
import kotlinx.coroutines.flow.Flow

class MessagesRepository(private val firebaseClient: FirebaseClient) {

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

    fun observeUserChats(userEmail: String): Flow<List<com.secure.messenger.data.model.Chat>> {
        return firebaseClient.observeUserChats(userEmail)
    }

    suspend fun getUserByEmail(email: String): User? {
        return firebaseClient.getUserByEmail(email)
    }

    suspend fun getChat(chatId: String): com.secure.messenger.data.model.Chat? {
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
