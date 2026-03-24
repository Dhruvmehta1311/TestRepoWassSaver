package com.pbhadoo.wassaver.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.pbhadoo.wassaver.data.model.MediaFilter
import com.pbhadoo.wassaver.data.model.MediaItem
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaBrowserScreen(navController: NavController) {
    val context = LocalContext.current
    var filter by remember { mutableStateOf(MediaFilter.ALL) }
    var items by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<MediaItem?>(null) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermission = results.values.any { it }
        if (hasPermission) {
            items = loadPrivateMedia(context)
        }
    }

    LaunchedEffect(Unit) {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permLauncher.launch(perms)
    }

    if (selectedItem != null) {
        FullscreenViewer(
            item = selectedItem!!,
            onDismiss = { selectedItem = null },
            onSave = { /* save logic */ },
            onShare = { item ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = if (item.isVideo) "video/*" else "image/*"
                    putExtra(Intent.EXTRA_STREAM, item.uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share via"))
            },
            onRepost = { }
        )
        return
    }

    val filtered = when (filter) {
        MediaFilter.ALL -> items
        MediaFilter.PHOTOS -> items.filter { !it.isVideo }
        MediaFilter.VIDEOS -> items.filter { it.isVideo }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Media Browser", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
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
                        Icon(Icons.Default.FolderOff, null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Text("No private media found",
                            style = MaterialTheme.typography.titleMedium)
                        Text("WhatsApp private media will appear here",
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
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable { selectedItem = item }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(item.uri)
                                    .apply { if (item.isVideo) videoFrameMillis(1000) }
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

fun loadPrivateMedia(context: android.content.Context): List<MediaItem> {
    val result = mutableListOf<MediaItem>()
    val base = Environment.getExternalStorageDirectory()
    val dirs = listOf(
        File(base, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/Private"),
        File(base, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video/Private"),
        File(base, "WhatsApp/Media/WhatsApp Images/Private"),
        File(base, "WhatsApp/Media/WhatsApp Video/Private")
    )
    dirs.forEach { dir ->
        if (dir.exists()) {
            dir.listFiles()?.forEach { file ->
                if (!file.name.startsWith(".")) {
                    val isVideo = file.extension.lowercase() in listOf("mp4", "mkv", "3gp")
                    result.add(MediaItem(
                        uri = Uri.fromFile(file),
                        name = file.name,
                        isVideo = isVideo,
                        size = file.length(),
                        dateModified = file.lastModified()
                    ))
                }
            }
        }
    }
    return result.sortedByDescending { it.dateModified }
}
