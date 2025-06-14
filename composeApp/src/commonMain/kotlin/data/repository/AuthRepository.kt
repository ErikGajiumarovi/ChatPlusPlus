package data.repository

import data.FirebaseClient
import data.NewFirebaseClient
import data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepository(private val firebaseClient: NewFirebaseClient) {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    suspend fun initialize() {
        println("Initializing AuthRepository | ${firebaseClient.getCurrentUser()?.email}")
        _currentUser.value = firebaseClient.getCurrentUser()
    }

    suspend fun signIn(email: String, password: String): Result<User> {
        val result = firebaseClient.signIn(email, password)
        if (result.isSuccess) {
            _currentUser.value = result.getOrNull()
        }
        return result
    }

    suspend fun signUp(email: String, password: String): Result<User> {
        val result = firebaseClient.signUp(email, password)
        if (result.isSuccess) {
            _currentUser.value = result.getOrNull()
        }
        return result
    }

    suspend fun signOut() {
        firebaseClient.signOut()
        _currentUser.value = null
    }

    fun isAuthenticated(): Boolean {
        return _currentUser.value != null
    }
}
