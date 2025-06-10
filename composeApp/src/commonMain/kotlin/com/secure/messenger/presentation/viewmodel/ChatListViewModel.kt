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
                messagesRepository.observeUserChats(currentUser.id).collectLatest { chats ->
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

            // Create a list of all participant IDs including current user
            val allParticipantIds = mutableListOf(currentUser.id)

            // In a real app, you would search for users by email and add their IDs
            // For this example, we'll just use emails as IDs for simplicity
            allParticipantIds.addAll(participantEmails)

            val result = messagesRepository.createChat(allParticipantIds, chatName)
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
