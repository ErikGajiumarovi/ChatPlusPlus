package com.secure.messenger.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secure.messenger.data.model.User
import com.secure.messenger.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Initial)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.initialize()
            if (authRepository.isAuthenticated()) {
                _uiState.value = LoginUiState.Success(authRepository.currentUser.value!!)
            }
        }
    }

    fun signIn(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Имя пользователя и пароль не могут быть пустыми")
            return
        }

        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            val result = authRepository.signIn(username, password)
            _uiState.value = if (result.isSuccess) {
                LoginUiState.Success(result.getOrNull()!!)
            } else {
                LoginUiState.Error(result.exceptionOrNull()?.message ?: "Ошибка авторизации")
            }
        }
    }

    fun signUp(username: String, password: String, confirmPassword: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Имя пользователя и пароль не могут быть пустыми")
            return
        }

        if (password != confirmPassword) {
            _uiState.value = LoginUiState.Error("Пароли не совпадают")
            return
        }

        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            val result = authRepository.signUp(username, password)
            _uiState.value = if (result.isSuccess) {
                LoginUiState.Success(result.getOrNull()!!)
            } else {
                LoginUiState.Error(result.exceptionOrNull()?.message ?: "Ошибка регистрации")
            }
        }
    }

    fun signInAnonymously() {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            val result = authRepository.signInAnonymously()
            _uiState.value = if (result.isSuccess) {
                LoginUiState.Success(result.getOrNull()!!)
            } else {
                LoginUiState.Error(result.exceptionOrNull()?.message ?: "Ошибка анонимной авторизации")
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
