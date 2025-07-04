package presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import data.model.Message
import data.repository.AuthRepository
import presentation.viewmodel.ChatUiState
import presentation.viewmodel.ChatViewModel
import util.DateFormatter
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import presentation.components.AppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel,
    authRepository: AuthRepository = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val messageInput by viewModel.messageInput.collectAsState()
    val chatData by viewModel.chatData.collectAsState()
    val currentUser by authRepository.currentUser.collectAsState()
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val dateFormatter = remember { DateFormatter() }

    // Generate a display name for the chat
    val chatDisplayName = remember(chatData) {
        when {
            // If chat has a custom name, use it
            !chatData?.name.isNullOrBlank() -> chatData?.name

            // For direct messages (2 participants), show the other person's email
            chatData?.participantEmails?.size == 2 -> {
                val otherParticipantEmail = chatData?.participantEmails?.firstOrNull {
                    it != currentUser?.email
                } ?: ""
                if (otherParticipantEmail.isNotEmpty()) otherParticipantEmail else "Chat"
            }

            // For group chats, show participant count
            chatData?.participantEmails?.isNotEmpty() == true ->
                "Group (${chatData?.participantEmails?.size ?: 0} participants)"

            // Fallback
            else -> "Chat"
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is ChatUiState.Success) {
            val messages = (uiState as ChatUiState.Success).messages
            if (messages.isNotEmpty()) {
                coroutineScope.launch {
                    listState.scrollToItem(messages.size - 1)
                }
            }
        }
    }

    var showEncryptionDialog by remember { mutableStateOf(false) }
    var encryptionKeyInput by remember { mutableStateOf("") }

    if (showEncryptionDialog) {
        AlertDialog(
            onDismissRequest = { showEncryptionDialog = false },
            title = { Text("Set Encryption Key") },
            text = {
                TextField(
                    value = encryptionKeyInput,
                    onValueChange = { encryptionKeyInput = it },
                    label = { Text("Encryption Key") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setEncryptionKey(encryptionKeyInput)
                    showEncryptionDialog = false
                }) {
                    Text("Set")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEncryptionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(chatDisplayName ?: "Chat") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(AppIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEncryptionDialog = true }) {
                        Icon(AppIcons.Encryption, contentDescription = "Set Encryption Key")
                    }
                }
            )
        },
        bottomBar = {
            MessageInputBar(
                value = messageInput,
                onValueChange = viewModel::onMessageInputChange,
                onSendClick = {
                    viewModel.sendMessage()
                    focusManager.clearFocus()
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is ChatUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ChatUiState.Success -> {
                    if (state.messages.isEmpty()) {
                        EmptyChatMessage(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        MessageList(
                            messages = state.messages,
                            listState = listState
                        )
                    }
                }
                is ChatUiState.Error -> {
                    ErrorMessage(
                        message = state.message,
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageList(
    messages: List<Message>,
    listState: LazyListState
) {
    val authRepository = koinInject<AuthRepository>()
    val currentUser by authRepository.currentUser.collectAsState()
    val currentUserEmail = currentUser?.email ?: ""

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        state = listState
    ) {
        items(messages) { message ->
            MessageItem(
                message = message,
                isFromCurrentUser = message.senderEmail == currentUserEmail
            )
        }
    }
}

@Composable
private fun MessageItem(
    message: Message,
    isFromCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { DateFormatter() }
    val backgroundColor = if (isFromCurrentUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isFromCurrentUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val alignment = if (isFromCurrentUser) {
        Alignment.End
    } else {
        Alignment.Start
    }
    val shape = if (isFromCurrentUser) {
        RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
    } else {
        RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .background(backgroundColor)
                .padding(12.dp)
        ) {
            Text(
                text = message.content,
                color = contentColor
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = dateFormatter.formatTime(message.timestamp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun MessageInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Surface(
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Type a message") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { onSendClick() }
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSendClick,
                enabled = value.isNotBlank()
            ) {
                Icon(
                    imageVector = AppIcons.Send,
                    contentDescription = "Send",
                    tint = if (value.isNotBlank()) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
    }
}

@Composable
private fun EmptyChatMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No messages yet",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Start the conversation by sending a message",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
