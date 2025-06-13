package data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton класс для отслеживания активного чата
 */
object ActiveChatTracker {
    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    /**
     * Устанавливает ID активного чата (чат, который сейчас открыт и просматривается)
     */
    fun setActiveChat(chatId: String?) {
        _activeChatId.value = chatId
    }

    /**
     * Проверяет, является ли указанный чат активным
     */
    fun isActiveChat(chatId: String): Boolean {
        return _activeChatId.value == chatId
    }
}
