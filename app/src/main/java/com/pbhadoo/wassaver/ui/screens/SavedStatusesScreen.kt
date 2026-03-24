package com.pbhadoo.wassaver.ui.screens

import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
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
import com.pbhadoo.wassaver.data.model.MediaFilter
import com.pbhadoo.wassaver.data.model.MediaItem
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedStatusesScreen(navController: NavController) {
    val context = LocalContext.current
    var filter by remember { mutableStateOf(MediaFilter.ALL) }
    var items by remember { mutableStateOf(loadSavedStatuses()) }
    var selectedSet by remember { mutableStateOf<Set<String>>(emptySet()) }
    var multiSelect by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<MediaItem?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    fun refresh() { items = loadSavedStatuses() }

    val filtered = when (filter) {
        MediaFilter.ALL -> items
        MediaFilter.PHOTOS -> items.filter { !it.isVideo }
        MediaFilter.VIDEOS -> items.filter { it.isVideo }
    }

    if (selectedItem != null) {
        FullscreenViewer(
            item = selectedItem!!,
            onDismiss = { selectedItem = null },
            onSave = { },
            onShare = { item ->
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = if (item.isVideo) "video/*" else "image/*"
                    putExtra(android.content.Intent.EXTRA_STREAM, item.uri)
                }
                context.startActivity(android.content.Intent.createChooser(intent, "Share via"))
            },
            onRepost = { }
        )
        return
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ${selectedSet.size} items?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    selectedSet.forEach { name ->
                        items.firstOrNull { it.name == name }?.let {
                            File(it.uri.path ?: return@let).delete()
                        }
                    }
                    selectedSet = emptySet()
                    multiSelect = false
                    showDeleteDialog = false
                    refresh()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (multiSelect) "${selectedSet.size} selected" else "Saved Statuses",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (multiSelect) { multiSelect = false; selectedSet = emptySet() }
                        else navController.navigateUp()
                    }) {
                        Icon(if (multiSelect) Icons.Default.Close else Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (multiSelect && selectedSet.isNotEmpty()) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, "Delete selected",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    } else if (!multiSelect && items.isNotEmpty()) {
                        IconButton(onClick = { multiSelect = true }) {
                            Icon(Icons.Default.SelectAll, "Multi-select")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MediaFilter.values().forEach { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { filter = f },
                        label = { Text(f.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.BookmarkBorder, null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Text("No saved statuses", style = MaterialTheme.typography.titleMedium)
                        Text("Save statuses from the Status Viewer",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(filtered) { item ->
                        val isSelected = selectedSet.contains(item.name)
                        Box {
                            StatusGridItem(
                                item = item,
                                onClick = {
                                    if (multiSelect) {
                                        selectedSet = if (isSelected)
                                            selectedSet - item.name
                                        else selectedSet + item.name
                                    } else {
                                        selectedItem = item
                                    }
                                },
                                onSave = {}
                            )
                            if (multiSelect) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        selectedSet = if (it) selectedSet + item.name
                                        else selectedSet - item.name
                                    },
                                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun loadSavedStatuses(): List<MediaItem> {
    val dir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
        "WASSaver"
    )
    if (!dir.exists()) return emptyList()
    return dir.listFiles()
        ?.filter { !it.name.startsWith(".") }
        ?.map { file ->
            val isVideo = file.extension.lowercase() in listOf("mp4", "mkv", "3gp")
            MediaItem(
                uri = Uri.fromFile(file),
                name = file.name,
                isVideo = isVideo,
                size = file.length(),
                dateModified = file.lastModified()
            )
        }
        ?.sortedByDescending { it.dateModified }
        ?: emptyList()
}
