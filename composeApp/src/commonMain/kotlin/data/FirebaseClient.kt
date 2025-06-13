// commonMain/kotlin/com/secure/messenger/data/FirebaseClient.kt
package data

import data.model.Chat
import data.model.Message
import data.model.User
import data.repository.ActiveChatTracker
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Clock
import dev.gitlive.firebase.auth.FirebaseUser as KMPFirebaseUser

class NewFirebaseClient {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private val usersCollection: CollectionReference by lazy { firestore.collection("users") }
    private val messagesCollection: CollectionReference by lazy { firestore.collection("messages") }
    private val chatsCollection: CollectionReference by lazy { firestore.collection("chats") }

    // Authentication Methods
    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password)
            val user = getUserFromFirebaseUser(authResult.user!!)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password)
            val firebaseUser = authResult.user!!

            // Create user document in Firestore
            val user = User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName ?: email.substringBefore('@')
            )

            usersCollection.document(user.id).set(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        auth.signOut()
    }

    suspend fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return getUserFromFirebaseUser(firebaseUser)
    }

    private suspend fun getUserFromFirebaseUser(firebaseUser: KMPFirebaseUser): User {
        val userDoc = usersCollection.document(firebaseUser.uid).get()
        return if (userDoc.exists) {
            User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = userDoc.get<String?>("displayName") ?: firebaseUser.displayName
                ?: firebaseUser.email?.substringBefore('@') ?: ""
            )
        } else {
            User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName ?: firebaseUser.email?.substringBefore('@') ?: ""
            )
        }
    }

    suspend fun getUserByEmail(email: String): User? {
        return try {
            val querySnapshot = usersCollection
                .limit(1)
                .get()

            if (querySnapshot.documents.isEmpty()) return null

            val doc = querySnapshot.documents.first()
            User(
                id = doc.id,
                email = doc.get<String?>("email") ?: "",
                displayName = doc.get<String?>("displayName") ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }

    // Message Methods
    suspend fun sendMessage(message: Message) {
        // Add message to Firestore
        messagesCollection.add(message)

        // Update last message in chat
        chatsCollection.document(message.chatId).update(
            mapOf(
                "lastMessageContent" to message.content,
                "lastMessageTimestamp" to message.timestamp
            )
        )

        // After sending a message, update unread count for all participants except sender
        updateUnreadCount(message.chatId, message.senderEmail)
    }

    fun observeMessages(chatId: String): Flow<List<Message>> {
        println("observeMessages | $chatId")
        return messagesCollection
            .orderBy("timestamp")
            .snapshots // this is suspend-based extension for dev.gitlive
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    try {
                        println("Mapping document: ${doc.id}")
                        val a = doc.data<Message>()
                        if (a.chatId.equals(chatId)) {
                            a.copy(id = doc.id)
                        } else {
                            println("Document ${doc.id} with ${a.chatId} does not belong to chat $chatId, filtering out.")
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
            }
    }

    suspend fun createChat(
        participantEmails: List<String>,
        name: String?
    ): String {
        // Initialize maps for all participants
        val initialLastReadMap = participantEmails.associateWith { 0L }
        val initialUnreadMap = participantEmails.associateWith { 0 }

        val chat = Chat(
            participantEmails = participantEmails,
            name = name,
            isGroupChat = participantEmails.size > 2,
            lastReadTimestamp = initialLastReadMap,
            unreadMessagesByUser = initialUnreadMap,
            lastMessageTimestamp = Clock.System.now().toEpochMilliseconds()
        )

        val docRef = chatsCollection.add(chat)
        return docRef.id
    }

    fun observeUserChats(userEmail: String): Flow<List<Chat>> {
        println("observeUserChats | $userEmail")
        return chatsCollection
            .orderBy("lastMessageTimestamp", Direction.DESCENDING)
            .snapshots
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    try {
                        val chat = doc.data<Chat>()
                        println("Mapping chat document: ${doc.id}")
                        if (chat.participantEmails.contains(userEmail)) {
                            chat.copy(id = doc.id)
                        } else {
                            println("Document ${doc.id} with ${chat.participantEmails} does not belong to userEmail $userEmail, filtering out.")
                            null
                        }
                    } catch (e: Exception) {
                        // --- Add detailed logging here ---
                        println("Error mapping chat document ${doc.id}: ${e.message}")
                        e.printStackTrace() // Print stack trace for more details
                        // ----------------------------------
                        null // Return null so mapNotNull filters it out
                    }
                }
            }
            .onCompletion { error ->
                if (error != null) {
                    // This will now show the specific 'Flow was aborted...' error if it still happens
                    println("Flow observation completed with error: ${error.message}")
                    error.printStackTrace() // Print stack trace if it's a different error
                } else {
                    println("Flow observation completed normally.")
                }
            }
            .onEach { chats ->
                // Optional: Log number of chats received
                println("Received ${chats.size} chats")
            }
    }

    suspend fun getChat(chatId: String): Chat? {
        val doc = chatsCollection.document(chatId).get()
        return if (doc.exists) {
            try {
                val chat = doc.data<Chat>()
                chat.copy(id = doc.id)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    // Unread Messages Methods
    suspend fun markChatAsRead(chatId: String, userEmail: String) {
        try {
            // Получаем текущий чат
            val chatDoc = chatsCollection.document(chatId).get()
            val chat = chatDoc.data<Chat>() ?: return

            // Обновляем timestamp последнего прочтения для конкретного пользователя
            val lastReadMap = chat.lastReadTimestamp.toMutableMap()
            lastReadMap[userEmail] = Clock.System.now().toEpochMilliseconds() // Using multiplatform time

            // Создаем мапу с непрочитанными сообщениями для каждого пользователя
            val unreadCountMap = chat.unreadMessagesByUser.toMutableMap()
            // Сбрасываем счетчик только для текущего пользователя
            unreadCountMap[userEmail] = 0

            // Обновляем в базе данных только поля для текущего пользователя
            chatsCollection.document(chatId).update(
                mapOf(
                    "lastReadTimestamp" to lastReadMap,
                    "unreadMessagesByUser" to unreadCountMap
                )
            )
        } catch (e: Exception) {
            println("Error marking chat as read: ${e.message}")
        }
    }

    suspend fun updateUnreadCount(chatId: String, senderEmail: String) {
        try {
            // Получаем текущий чат
            val chatDoc = chatsCollection.document(chatId).get()
            val chat = chatDoc.data<Chat>() ?: return

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
                        (participantEmail == currentUserEmail) && ActiveChatTracker.isActiveChat(chatId) // Assuming ActiveChatTracker is in commonMain

                    // Увеличиваем счетчик только если это не текущий пользователь в активном чате
                    if (!isCurrentUserInActiveChat) {
                        val currentCount = unreadCountMap[participantEmail] ?: 0
                        unreadCountMap[participantEmail] = currentCount + 1
                    }
                }
            }

            // Обновляем счетчики в базе данных
            chatsCollection.document(chatId).update(
                mapOf("unreadMessagesByUser" to unreadCountMap)
            )
        } catch (e: Exception) {
            println("Error updating unread count: ${e.message}")
        }
    }
}