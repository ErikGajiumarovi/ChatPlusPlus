package presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.model.Chat
import data.repository.AuthRepository
import data.repository.MessagesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatListViewModel(
    private val authRepository: AuthRepository,
    private val messagesRepository: MessagesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatListUiState>(ChatListUiState.Loading)
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    init {
        loadChats()
    }

    private fun loadChats() {
        viewModelScope.launch {
            val currentUser = authRepository.currentUser.value
            if (currentUser == null) {
                _uiState.value = ChatListUiState.Error("User not authenticated")
                return@launch
            }

            try {
                // Use email instead of ID for observing chats
                messagesRepository.observeUserChats(currentUser.email).collectLatest { chats ->
                    _uiState.value = ChatListUiState.Success(chats)
                }
            } catch (e: Exception) {
                _uiState.value = ChatListUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun createNewChat(participantEmails: List<String>, chatName: String? = null) {
        viewModelScope.launch {
            val currentUser = authRepository.currentUser.value
            if (currentUser == null) {
                _uiState.value = ChatListUiState.Error("User not authenticated")
                return@launch
            }

            // Проверяем существование всех указанных пользователей
            val nonExistingUsers = mutableListOf<String>()
            for (email in participantEmails) {
                if (email != currentUser.email) { // Пропускаем проверку текущего пользователя
                    val user = messagesRepository.getUserByEmail(email)
                    if (user == null) {
                        nonExistingUsers.add(email)
                    }
                }
            }

            // Если есть несуществующие пользователи, показываем ошибку
            if (nonExistingUsers.isNotEmpty()) {
                val errorMessage = if (nonExistingUsers.size == 1) {
                    "User with email ${nonExistingUsers[0]} does not exist"
                } else {
                    "The following users do not exist: ${nonExistingUsers.joinToString(", ")}"
                }
                _uiState.value = ChatListUiState.Error(errorMessage)
                return@launch
            }

            // Create a list of all participant emails including current user
            val allParticipantEmails = mutableListOf(currentUser.email)
            allParticipantEmails.addAll(participantEmails)

            val result = messagesRepository.createChat(allParticipantEmails, chatName)
            if (result.isFailure) {
                _uiState.value = ChatListUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to create chat"
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun refresh() {
        loadChats()
    }
}

sealed class ChatListUiState {
    object Loading : ChatListUiState()
    data class Success(val chats: List<Chat>) : ChatListUiState()
    data class Error(val message: String) : ChatListUiState()
}


