package com.secure.messenger.data.repository

import com.secure.messenger.data.FirebaseClient
import com.secure.messenger.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepository(private val firebaseClient: FirebaseClient) {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    suspend fun initialize() {
        _currentUser.value = firebaseClient.getCurrentUser()
    }

    suspend fun signIn(username: String, password: String): Result<User> {
        val result = firebaseClient.signIn(username, password)
        if (result.isSuccess) {
            _currentUser.value = result.getOrNull()
        }
        return result
    }

    suspend fun signUp(username: String, password: String): Result<User> {
        val result = firebaseClient.signUp(username, password)
        if (result.isSuccess) {
            _currentUser.value = result.getOrNull()
        }
        return result
    }

    suspend fun signInAnonymously(): Result<User> {
        val result = firebaseClient.signInAnonymously()
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
