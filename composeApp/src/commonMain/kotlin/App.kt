import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import di.AppModule
import presentation.ui.ChatListScreen
import presentation.ui.ChatScreen
import presentation.ui.LoginScreen
import presentation.viewmodel.ChatViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

enum class Screen {
    LOGIN,
    CHAT_LIST,
    CHAT
}

data class NavigationState(
    val currentScreen: Screen = Screen.LOGIN,
    val chatId: String? = null
)

class AppNavigation {
    private val _navigationState = MutableStateFlow(NavigationState())
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

    fun navigateToLogin() {
        _navigationState.value = NavigationState(Screen.LOGIN)
    }

    fun navigateToChatList() {
        _navigationState.value = NavigationState(Screen.CHAT_LIST)
    }

    fun navigateToChat(chatId: String) {
        _navigationState.value = NavigationState(Screen.CHAT, chatId)
    }
}

@Composable
fun App() {
    KoinApplication(application = {
        modules(AppModule.appModule())
    }) {
        val navigation = remember { AppNavigation() }
        val navState = navigation.navigationState.collectAsState().value

        when (navState.currentScreen) {
            Screen.LOGIN -> {
                LoginScreen(
                    onNavigateToChats = { navigation.navigateToChatList() }
                )
            }
            Screen.CHAT_LIST -> {
                ChatListScreen(
                    onNavigateToChat = { chatId -> navigation.navigateToChat(chatId) },
                    onNavigateToLogin = { navigation.navigateToLogin() }
                )
            }
            Screen.CHAT -> {
                val chatId = navState.chatId ?: return@KoinApplication
                val chatViewModel = koinInject<ChatViewModel> { parametersOf(chatId) }

                ChatScreen(
                    chatId = chatId,
                    onNavigateBack = { navigation.navigateToChatList() },
                    viewModel = chatViewModel
                )
            }
        }
    }
}
