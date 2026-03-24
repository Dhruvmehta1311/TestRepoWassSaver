package com.pbhadoo.wassaver.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pbhadoo.wassaver.utils.MessageStore
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeletedMessageChatScreen(navController: NavController, senderId: String) {
    val context = LocalContext.current
    var messages by remember { mutableStateOf(MessageStore.getMessagesForSender(context, senderId)) }
    val listState = rememberLazyListState()
    val senderName = messages.firstOrNull()?.senderName ?: "Unknown"

    // Auto-refresh every 3 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            val updated = MessageStore.getMessagesForSender(context, senderId)
            if (updated.size != messages.size) {
                messages = updated
                if (updated.isNotEmpty()) {
                    listState.animateScrollToItem(updated.size - 1)
                }
            }
        }
    }

    // Scroll to bottom on first load
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(senderName, fontWeight = FontWeight.Bold)
                        Text("${messages.size} deleted messages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        MessageStore.clearChat(context, senderId)
                        navController.navigateUp()
                    }) {
                        Icon(Icons.Default.Delete, "Clear chat",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        if (messages.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No messages found")
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(messages) { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 16.dp, topEnd = 4.dp,
                                bottomStart = 16.dp, bottomEnd = 16.dp
                            ),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                if (msg.isGroup && msg.groupName != null) {
                                    Text(
                                        "~ ${msg.groupName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(2.dp))
                                }
                                Text(msg.text, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                        .format(Date(msg.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
