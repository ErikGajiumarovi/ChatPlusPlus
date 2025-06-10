package com.secure.messenger.data

import com.secure.messenger.data.model.Chat
import com.secure.messenger.data.model.Message
import com.secure.messenger.data.model.User
import kotlinx.coroutines.flow.Flow

actual class FirebaseClient actual constructor() : FirebaseClientInterface {
    actual override suspend fun signIn(
        email: String,
        password: String
    ): Result<User> {
        TODO("Not yet implemented")
    }

    actual override suspend fun signUp(
        email: String,
        password: String
    ): Result<User> {
        TODO("Not yet implemented")
    }

    actual override suspend fun signOut() {
    }

    actual override suspend fun getCurrentUser(): User? {
        TODO("Not yet implemented")
    }

    actual override suspend fun sendMessage(message: Message) {
    }

    actual override fun observeMessages(chatId: String): Flow<List<Message>> {
        TODO("Not yet implemented")
    }

    actual override suspend fun createChat(
        participantIds: List<String>,
        name: String?
    ): String {
        TODO("Not yet implemented")
    }

    actual override fun observeUserChats(userId: String): Flow<List<Chat>> {
        TODO("Not yet implemented")
    }

    actual override suspend fun getChat(chatId: String): Chat? {
        TODO("Not yet implemented")
    }
}