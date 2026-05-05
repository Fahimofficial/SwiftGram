package com.swiftgram.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.swiftgram.app.ui.viewmodels.ChatListViewModel
import com.swiftgram.domain.model.ChatListItem
import java.text.SimpleDateFormat
import java.util.*

/**
 * Chat List screen displays all the user's chats.
 * Shows a list of chats with the last message preview and unread count.
 *
 * @param viewModel The chat list ViewModel
 * @param onChatSelected Callback when a chat is selected
 * @param onSettingsClick Callback when settings button is clicked
 */
@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel = hiltViewModel(),
    onChatSelected: (Long) -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val chats by viewModel.chats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    // Load chats on first composition
    LaunchedEffect(Unit) {
        viewModel.loadChats()
    }
    
    Scaffold(
        topBar = {
            ChatListTopBar(onSettingsClick = onSettingsClick)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Create new chat */ },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New Chat")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Error loading chats",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                chats.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No chats yet",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = "Start a conversation by tapping the + button",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(chats) { chat ->
                            ChatListItemComposable(
                                chat = chat,
                                onChatSelected = onChatSelected
                            )
                            Divider(
                                modifier = Modifier.padding(
                                    start = 72.dp,
                                    end = 16.dp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Top app bar for the chat list screen.
 */
@Composable
fun ChatListTopBar(onSettingsClick: () -> Unit = {}) {
    TopAppBar(
        title = {
            Text(
                text = "Chats",
                style = MaterialTheme.typography.headlineMedium
            )
        },
        actions = {
            IconButton(onClick = { /* TODO: Search */ }) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Settings")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

/**
 * Individual chat list item.
 */
@Composable
fun ChatListItemComposable(
    chat: ChatListItem,
    onChatSelected: (Long) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChatSelected(chat.id) }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Chat avatar
        if (chat.photoUrl != null) {
            AsyncImage(
                model = chat.photoUrl,
                contentDescription = chat.title,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chat.title.firstOrNull()?.uppercase() ?: "?",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Chat info
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            // Chat title
            Text(
                text = chat.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Last message preview
            Text(
                text = chat.lastMessage.ifEmpty { "No messages yet" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Timestamp and unread badge
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = formatTimestamp(chat.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (chat.unreadCount > 0) {
                Surface(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

/**
 * Format timestamp to a readable format.
 * Shows time for today, date for older messages.
 */
private fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return ""
    
    val date = Date(timestamp * 1000)
    val calendar = Calendar.getInstance().apply { time = date }
    val today = Calendar.getInstance()
    
    return when {
        calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
        calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> {
            // Today - show time
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        }
        calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
        calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) - 1 -> {
            // Yesterday
            "Yesterday"
        }
        calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) -> {
            // This year - show date
            SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
        }
        else -> {
            // Different year - show full date
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
        }
    }
}
