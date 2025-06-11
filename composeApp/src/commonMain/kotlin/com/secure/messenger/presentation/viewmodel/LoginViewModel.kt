package com.secure.messenger.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secure.messenger.data.model.Chat
import com.secure.messenger.data.model.User
import com.secure.messenger.data.repository.AuthRepository
import com.secure.messenger.data.repository.MessagesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val messagesRepository: MessagesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Initial)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.initialize()
            if (authRepository.isAuthenticated()) {
                val user = authRepository.currentUser.value!!
                // Проверяем наличие персонального чата при автоматическом входе
                checkAndCreatePersonalChat(user.email)
                _uiState.value = LoginUiState.Success(user)
            }
        }
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Email and password cannot be empty")
            return
        }

        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            val result = authRepository.signIn(email, password)
            _uiState.value = if (result.isSuccess) {
                val user = result.getOrNull()!!
                // Проверяем наличие персонального чата при каждом входе
                checkAndCreatePersonalChat(user.email)
                LoginUiState.Success(user)
            } else {
                LoginUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun signUp(email: String, password: String, confirmPassword: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Email and password cannot be empty")
            return
        }

        if (password != confirmPassword) {
            _uiState.value = LoginUiState.Error("Passwords do not match")
            return
        }

        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            val result = authRepository.signUp(email, password)
            _uiState.value = if (result.isSuccess) {
                val user = result.getOrNull()!!
                // Создаем персональный чат для нового пользователя
                createPersonalChat(user.email)
                LoginUiState.Success(user)
            } else {
                LoginUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    // Проверяет наличие персонального чата и создает его при необходимости
    private suspend fun checkAndCreatePersonalChat(userEmail: String) {
        try {
            // Получаем все чаты пользователя
            val userChats = messagesRepository.observeUserChats(userEmail).first()

            // Ищем персональный чат (чат, где единственный участник - сам пользователь)
            val hasPersonalChat = userChats.any { chat ->
                chat.participantEmails.size == 1 && chat.participantEmails.contains(userEmail)
            }

            // Если персональный чат не найден, создаем его
            if (!hasPersonalChat) {
                createPersonalChat(userEmail)
            }
        } catch (e: Exception) {
            // Если произошла ошибка при проверке, логируем ее, но не прерываем процесс входа
            println("Failed to check for personal chat: ${e.message}")
        }
    }

    // Вспомогательный метод для создания персонального чата
    private suspend fun createPersonalChat(userEmail: String) {
        try {
            // Создаем чат, где единственный участник - сам пользователь
            val participantEmails = listOf(userEmail)
            messagesRepository.createChat(participantEmails, "My Notes")
        } catch (e: Exception) {
            // Если не удалось создать чат, логируем ошибку, но не прерываем процесс входа
            println("Failed to create personal chat: ${e.message}")
        }
    }

    fun signInAnonymously() {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            val result = authRepository.signInAnonymously()
            _uiState.value = if (result.isSuccess) {
                LoginUiState.Success(result.getOrNull()!!)
            } else {
                LoginUiState.Error(result.exceptionOrNull()?.message ?: "Error with anonymous sign in")
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Initial
    }
}

sealed class LoginUiState {
    object Initial : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val user: User) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}
