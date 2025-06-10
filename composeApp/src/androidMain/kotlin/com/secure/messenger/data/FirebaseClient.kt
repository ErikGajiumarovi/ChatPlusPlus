package com.secure.messenger.data

import com.google.firebase.auth.FirebaseUser as GoogleFirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.secure.messenger.data.model.Chat
import com.secure.messenger.data.model.Message
import com.secure.messenger.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

actual class FirebaseClient : FirebaseClientInterface {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    // Authentication Methods
    actual override suspend fun signIn(username: String, password: String): Result<User> {
        return try {
            // Сначала находим пользователя по username в Firestore
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                return Result.failure(Exception("Пользователь не найден"))
            }

            // Получаем email из документа пользователя для авторизации
            // (Firebase Auth все равно требует email)
            val userDoc = querySnapshot.documents.first()
            val email = userDoc.getString("email") ?: ""

            if (email.isEmpty()) {
                return Result.failure(Exception("Ошибка авторизации"))
            }

            // Авторизуемся с email и паролем
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = getUserFromFirebaseUser(authResult.user!!)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual override suspend fun signUp(username: String, password: String): Result<User> {
        return try {
            // Проверка, что username не занят
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                return Result.failure(Exception("Это имя пользователя уже занято"))
            }

            // Создаем email для Firebase Auth (требуется)
            val email = "$username@chatplusplus.app"

            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user!!

            // Обновляем профиль пользователя
            val profileUpdates = userProfileChangeRequest {
                displayName = username
            }
            firebaseUser.updateProfile(profileUpdates).await()

            // Создаем документ пользователя в Firestore
            val user = User(
                id = firebaseUser.uid,
                username = username,
                displayName = username
            )

            // Сохраняем email в документе для будущей авторизации
            val userData = mapOf(
                "id" to user.id,
                "username" to user.username,
                "displayName" to user.displayName,
                "email" to email
            )

            firestore.collection("users").document(user.id).set(userData).await()
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
                username = "",
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
                username = userDoc.getString("username") ?: "",
                displayName = userDoc.getString("displayName") ?: ""
            )
        } else {
            User(
                id = firebaseUser.uid,
                username = "",
                displayName = firebaseUser.displayName ?: ""
            )
        }
    }

    // Message Methods
    actual override suspend fun sendMessage(message: Message) {
        firestore.collection("messages").add(message).await()

        // Update last message in chat
        firestore.collection("chats").document(message.chatId).update(
            mapOf(
                "lastMessageContent" to message.content,
                "lastMessageTimestamp" to message.timestamp
            )
        ).await()
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
    actual override suspend fun createChat(participantIds: List<String>, name: String?): String {
        val chat = Chat(
            participantIds = participantIds,
            name = name,
            isGroupChat = participantIds.size > 2
        )

        val docRef = firestore.collection("chats").add(chat).await()
        return docRef.id
    }

    actual override fun observeUserChats(userId: String): Flow<List<Chat>> = callbackFlow {
        val listenerRegistration = firestore.collection("chats")
            .whereArrayContains("participantIds", userId)
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
}
