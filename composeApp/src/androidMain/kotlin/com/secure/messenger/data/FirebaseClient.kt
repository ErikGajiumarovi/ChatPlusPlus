package com.secure.messenger.data

import com.google.firebase.auth.FirebaseUser as GoogleFirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.secure.messenger.data.model.Chat
import com.secure.messenger.data.model.Message
import com.secure.messenger.data.model.User
import com.secure.messenger.data.repository.ActiveChatTracker
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

actual class FirebaseClient : FirebaseClientInterface {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    // Authentication Methods
    actual override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = getUserFromFirebaseUser(authResult.user!!)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual override suspend fun signUp(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user!!

            // Create user document in Firestore
            val user = User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName ?: email.substringBefore('@')
            )

            firestore.collection("users").document(user.id).set(user).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual override suspend fun signInAnonymously(): Result<User> {
        return try {
            val authResult = auth.signInAnonymously().await()
            val firebaseUser = authResult.user!!

            // Создаем анонимного пользователя
            val user = User(
                id = firebaseUser.uid,
                email = "",
                displayName = "Гость ${firebaseUser.uid.takeLast(5)}" // Используем часть UID как имя гостя
            )

            // Сохраняем информацию о пользователе в Firestore
            firestore.collection("users").document(user.id).set(user).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual override suspend fun signOut() {
        auth.signOut()
    }

    actual override suspend fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return getUserFromFirebaseUser(firebaseUser)
    }

    private suspend fun getUserFromFirebaseUser(firebaseUser: GoogleFirebaseUser): User {
        val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
        return if (userDoc.exists()) {
            User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = userDoc.getString("displayName") ?: firebaseUser.displayName ?: firebaseUser.email?.substringBefore('@') ?: ""
            )
        } else {
            User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName ?: firebaseUser.email?.substringBefore('@') ?: ""
            )
        }
    }

    actual override suspend fun getUserByEmail(email: String): User? {
        try {
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()

            if (querySnapshot.isEmpty) return null

            val doc = querySnapshot.documents[0]
            return User(
                id = doc.id,
                email = doc.getString("email") ?: "",
                displayName = doc.getString("displayName") ?: ""
            )
        } catch (e: Exception) {
            return null
        }
    }

    // Message Methods
    actual override suspend fun sendMessage(message: Message) {
        // Add message to Firestore
        firestore.collection("messages").add(message).await()

        // Update last message in chat
        firestore.collection("chats").document(message.chatId).update(
            mapOf(
                "lastMessageContent" to message.content,
                "lastMessageTimestamp" to message.timestamp
            )
        ).await()

        // After sending a message, update unread count for all participants except sender
        updateUnreadCount(message.chatId, message.senderEmail)
    }

    actual override fun observeMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listenerRegistration = firestore.collection("messages")
            .whereEqualTo("chatId", chatId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Message::class.java)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listenerRegistration.remove() }
    }

    // Chat Methods
    actual override suspend fun createChat(participantEmails: List<String>, name: String?): String {
        val chat = Chat(
            participantEmails = participantEmails,
            name = name,
            isGroupChat = participantEmails.size > 2
        )

        val docRef = firestore.collection("chats").add(chat).await()
        return docRef.id
    }

    actual override fun observeUserChats(userEmail: String): Flow<List<Chat>> = callbackFlow {
        val listenerRegistration = firestore.collection("chats")
            .whereArrayContains("participantEmails", userEmail)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val chats = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val chat = doc.toObject(Chat::class.java)
                        chat?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(chats)
            }

        awaitClose { listenerRegistration.remove() }
    }

    actual override suspend fun getChat(chatId: String): Chat? {
        val doc = firestore.collection("chats").document(chatId).get().await()
        return if (doc.exists()) {
            val chat = doc.toObject(Chat::class.java)
            chat?.copy(id = doc.id)
        } else {
            null
        }
    }

    // Unread Messages Methods
    actual override suspend fun markChatAsRead(chatId: String, userEmail: String) {
        try {
            // Получаем текущий чат
            val chatDoc = firestore.collection("chats").document(chatId).get().await()
            val chat = chatDoc.toObject(Chat::class.java) ?: return

            // Обновляем timestamp последнего прочтения для конкретного пользователя
            val lastReadMap = chat.lastReadTimestamp.toMutableMap()
            lastReadMap[userEmail] = System.currentTimeMillis()

            // Создаем мапу с непрочитанными сообщениями для каждого пользователя
            val unreadCountMap = chat.unreadMessagesByUser.toMutableMap()
            // Сбрасываем счетчик только для текущего пользователя
            unreadCountMap[userEmail] = 0

            // Обновляем в базе данных только поля для текущего пользователя
            firestore.collection("chats").document(chatId).update(
                mapOf(
                    "lastReadTimestamp" to lastReadMap,
                    "unreadMessagesByUser" to unreadCountMap
                )
            ).await()
        } catch (e: Exception) {
            println("Error marking chat as read: ${e.message}")
        }
    }

    actual override suspend fun updateUnreadCount(chatId: String, senderEmail: String) {
        try {
            // Получаем текущий чат
            val chatDoc = firestore.collection("chats").document(chatId).get().await()
            val chat = chatDoc.toObject(Chat::class.java) ?: return

            // Получаем текущие значения счетчиков для каждого пользователя
            val unreadCountMap = chat.unreadMessagesByUser.toMutableMap()

            // Получаем текущего пользователя
            val currentUser = getCurrentUser()
            val currentUserEmail = currentUser?.email ?: ""

            // Увеличиваем счетчик для всех пользователей, кроме отправителя
            // и кроме текущего пользователя, если чат активен
            chat.participantEmails.forEach { participantEmail ->
                if (participantEmail != senderEmail) {
                    // Проверяем, является ли этот участник текущим пользователем и активен ли чат
                    val isCurrentUserInActiveChat =
                        (participantEmail == currentUserEmail) && ActiveChatTracker.isActiveChat(chatId)

                    // Увеличиваем счетчик только если это не текущий пользователь в активном чате
                    if (!isCurrentUserInActiveChat) {
                        val currentCount = unreadCountMap[participantEmail] ?: 0
                        unreadCountMap[participantEmail] = currentCount + 1
                    }
                }
            }

            // Обновляем счетчики в базе данных
            firestore.collection("chats").document(chatId).update(
                "unreadMessagesByUser", unreadCountMap
            ).await()
        } catch (e: Exception) {
            println("Error updating unread count: ${e.message}")
        }
    }
}
