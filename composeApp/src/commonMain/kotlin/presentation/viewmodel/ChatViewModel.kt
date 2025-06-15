package presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.model.Chat
import data.model.Message
import data.repository.ActiveChatTracker
import data.repository.AuthRepository
import data.repository.MessagesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import util.AESCrypto

class ChatViewModel(
    private val chatId: String,
    private val authRepository: AuthRepository,
    private val messagesRepository: MessagesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _messageInput = MutableStateFlow("")
    val messageInput: StateFlow<String> = _messageInput.asStateFlow()

    private val _chatData = MutableStateFlow<Chat?>(null)
    val chatData: StateFlow<Chat?> = _chatData.asStateFlow()

    private var encryptionKey: String? = null
    private var aesCrypto: AESCrypto? = null

    init {
        // Устанавливаем текущий чат как активный
        ActiveChatTracker.setActiveChat(chatId)

        loadMessages()
        loadChatData()
    }

    fun setEncryptionKey(key: String) {
        encryptionKey = key
        aesCrypto = AESCrypto(key)
    }

    override fun onCleared() {
        super.onCleared()
        // Когда ViewModel уничтожается (чат закрывается), сбрасываем активный чат
        ActiveChatTracker.setActiveChat(null)
        encryptionKey = null
        aesCrypto = null
    }

    private fun loadChatData() {
        viewModelScope.launch {
            try {
                val chat = messagesRepository.getChat(chatId)
                _chatData.value = chat
            } catch (e: Exception) {
                // Chat data couldn't be loaded, but we can still show messages
            }
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            try {
                // Отмечаем чат как прочитанный при открытии
                messagesRepository.markChatAsRead(chatId)

                messagesRepository.observeMessages(chatId).collectLatest { messages ->
                    val processedMessages = messages.map { message: Message ->
                        if (message.isEncrypted) {
                            try {
                                val decryptedContent = aesCrypto?.decrypt(message.content) ?: message.content
                                message.copy(content = decryptedContent)
                            } catch (e: Exception) {
                                message.copy(content = "decrypt error")
                            }
                        } else {
                            message
                        }
                    }
                    _uiState.value = ChatUiState.Success(processedMessages)
                }
            } catch (e: Exception) {
                _uiState.value = ChatUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun onMessageInputChange(input: String) {
        _messageInput.value = input
    }

    fun sendMessage() {
        val messageContent = _messageInput.value.trim()
        if (messageContent.isBlank()) return

        viewModelScope.launch {
            val result: Result<Unit>
            if (aesCrypto != null) {
                val encryptedContent = aesCrypto?.encrypt(messageContent) ?: messageContent
                result = messagesRepository.sendMessage(chatId, true, encryptedContent)
            } else {
                result = messagesRepository.sendMessage(chatId, false, messageContent)
            }
            if (result.isSuccess) {
                _messageInput.value = ""
            } else {
                _uiState.value = ChatUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to send message"
                )
            }
        }
    }

    fun refresh() {
        loadMessages()
        loadChatData()
    }
}

sealed class ChatUiState {
    object Loading : ChatUiState()
    data class Success(val messages: List<Message>) : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}
