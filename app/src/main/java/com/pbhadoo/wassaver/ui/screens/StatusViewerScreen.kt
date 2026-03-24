package com.pbhadoo.wassaver.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.pbhadoo.wassaver.data.model.MediaFilter
import com.pbhadoo.wassaver.data.model.MediaItem
import com.pbhadoo.wassaver.data.model.WaApp
import com.pbhadoo.wassaver.viewmodel.StatusViewerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusViewerScreen(navController: NavController) {
    val vm: StatusViewerViewModel = viewModel()
    val state by vm.uiState.collectAsState()
    val filtered by vm.filteredItems.collectAsState()
    val context = LocalContext.current

    var selectedItem by remember { mutableStateOf<MediaItem?>(null) }

    // SAF launcher for Android 11+ folder access
    val safLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            vm.onSafUriGranted(uri, state.waApp)
        }
    }

    LaunchedEffect(Unit) { vm.loadStatuses() }

    state.message?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(2000)
            vm.clearMessage()
        }
    }

    if (selectedItem != null) {
        FullscreenViewer(
            item = selectedItem!!,
            onDismiss = { selectedItem = null },
            onSave = { vm.saveMedia(it) },
            onShare = { vm.shareMedia(context, it) },
            onRepost = { vm.repostToWhatsApp(context, it) }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Status Viewer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // WA / WAB toggle
                    FilterChip(
                        selected = state.waApp == WaApp.WHATSAPP,
                        onClick = { vm.setWaApp(WaApp.WHATSAPP) },
                        label = { Text("WA") },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    FilterChip(
                        selected = state.waApp == WaApp.BUSINESS,
                        onClick = { vm.setWaApp(WaApp.BUSINESS) },
                        label = { Text("WAB") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            )
        },
        snackbarHost = {
            state.message?.let {
                Snackbar(modifier = Modifier.padding(16.dp)) { Text(it) }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Filter tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MediaFilter.values().forEach { filter ->
                    FilterChip(
                        selected = state.filter == filter,
                        onClick = { vm.setFilter(filter) },
                        label = { Text(filter.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.needsPermission -> {
                    PermissionCard(
                        waApp = state.waApp,
                        onGrantPermission = {
                            val path = when (state.waApp) {
                                WaApp.WHATSAPP -> "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"
                                WaApp.BUSINESS -> "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"
                            }
                            safLauncher.launch(null)
                        }
                    )
                }
                filtered.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(16.dp))
                            Text("No statuses found", style = MaterialTheme.typography.titleMedium)
                            Text("Open WhatsApp and view some statuses first",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(filtered) { item ->
                            StatusGridItem(
                                item = item,
                                onClick = { selectedItem = item },
                                onSave = { vm.saveMedia(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusGridItem(item: MediaItem, onClick: () -> Unit, onSave: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.uri)
                .apply { if (item.isVideo) videoFrameMillis(1000) }
                .crossfade(true)
                .build(),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Badge
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp),
            color = if (item.isVideo) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Icon(
                if (item.isVideo) Icons.Default.Videocam else Icons.Default.Image,
                contentDescription = null,
                modifier = Modifier.padding(2.dp).size(12.dp),
                tint = Color.White
            )
        }
        // Save button
        IconButton(
            onClick = onSave,
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Icon(
                Icons.Default.SaveAlt,
                contentDescription = "Save",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun PermissionCard(waApp: WaApp, onGrantPermission: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.padding(24.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text("Folder Access Required", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Android 11+ requires you to manually grant access to the WhatsApp status folder.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onGrantPermission) {
                    Icon(Icons.Default.Folder, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Grant Access")
                }
            }
        }
    }
}
