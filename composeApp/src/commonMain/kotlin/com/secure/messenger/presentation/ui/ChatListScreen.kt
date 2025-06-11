package com.secure.messenger.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.secure.messenger.data.model.Chat
import com.secure.messenger.data.repository.AuthRepository
import com.secure.messenger.presentation.viewmodel.ChatListUiState
import com.secure.messenger.presentation.viewmodel.ChatListViewModel
import com.secure.messenger.ui.components.AppIcons
import com.secure.messenger.util.DateFormatter
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onNavigateToChat: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: ChatListViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showNewChatDialog by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateFormatter() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat++") },
                actions = {
                    IconButton(onClick = {
                        viewModel.signOut()
                        onNavigateToLogin()
                    }) {
                        Icon(
                            imageVector = AppIcons.ExitToApp,
                            contentDescription = "Sign Out"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewChatDialog = true }) {
                Icon(
                    imageVector = AppIcons.Add,
                    contentDescription = "New Chat"
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is ChatListUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ChatListUiState.Success -> {
                    if (state.chats.isEmpty()) {
                        EmptyChatList(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        ChatList(
                            chats = state.chats,
                            onChatClick = onNavigateToChat
                        )
                    }
                }
                is ChatListUiState.Error -> {
                    ErrorMessage(
                        message = state.message,
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }

    if (showNewChatDialog) {
        NewChatDialog(
            onDismiss = { showNewChatDialog = false },
            onCreateChat = { emails, name ->
                viewModel.createNewChat(emails, name)
                showNewChatDialog = false
            }
        )
    }
}

@Composable
private fun ChatList(
    chats: List<Chat>,
    onChatClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(chats) { chat ->
            ChatItem(
                chat = chat,
                onClick = { onChatClick(chat.id) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatItem(
    chat: Chat,
    onClick: () -> Unit
) {
    val dateFormatter = remember { DateFormatter() }
    val authRepository = koinInject<AuthRepository>()
    val currentUser by authRepository.currentUser.collectAsState()
    val currentUserEmail = currentUser?.email ?: ""

    // Определяем отображаемое имя чата
    val displayName = remember(chat, currentUserEmail) {
        when {
            // Если у чата есть название, используем его
            !chat.name.isNullOrBlank() -> chat.name

            // Для личного чата с самим собой
            chat.participantEmails.size == 1 && chat.participantEmails.contains(currentUserEmail) ->
                "Мои заметки"

            // Для чата с двумя участниками показываем email другого участника
            chat.participantEmails.size == 2 -> {
                chat.participantEmails.firstOrNull { it != currentUserEmail } ?:
                    "Chat with ${chat.participantEmails.size} participants"
            }

            // Для групповых чатов
            else -> "Chat with ${chat.participantEmails.size} participants"
        }
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                // Отображаем счетчик непрочитанных сообщений для текущего пользователя, если они есть
                val unreadCount = chat.unreadMessagesByUser[currentUserEmail] ?: 0
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.lastMessageContent ?: "No messages yet",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (chat.lastMessageTimestamp > 0) {
                    val formattedTime = remember(chat.lastMessageTimestamp) {
                        dateFormatter.formatTime(chat.lastMessageTimestamp)
                    }

                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyChatList(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No chats yet",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Start a new conversation by pressing the + button",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun ErrorMessage(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun NewChatDialog(
    onDismiss: () -> Unit,
    onCreateChat: (List<String>, String?) -> Unit
) {
    var emailInput by remember { mutableStateOf("") }
    var chatName by remember { mutableStateOf("") }
    val emailList = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Chat") },
        text = {
            Column {
                OutlinedTextField(
                    value = chatName,
                    onValueChange = { chatName = it },
                    label = { Text("Chat Name (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Participant Email") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (emailInput.isNotBlank()) {
                            emailList.add(emailInput)
                            emailInput = ""
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Add")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Added participants:",
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(4.dp))

                emailList.forEach { email ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = email,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = { emailList.remove(email) }
                        ) {
                            Text("✕")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (emailList.isNotEmpty()) {
                        onCreateChat(emailList.toList(), chatName.takeIf { it.isNotBlank() })
                    }
                },
                enabled = emailList.isNotEmpty()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
