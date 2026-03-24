package com.pbhadoo.wassaver.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.pbhadoo.wassaver.data.model.MediaItem

@Composable
fun FullscreenViewer(
    item: MediaItem,
    onDismiss: () -> Unit,
    onSave: (MediaItem) -> Unit,
    onShare: (MediaItem) -> Unit,
    onRepost: (MediaItem) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (item.isVideo) {
            VideoPlayerContent(uri = item.uri)
        } else {
            AsyncImage(
                model = item.uri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, "Close", tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            Text(item.name, color = Color.White, style = MaterialTheme.typography.bodySmall)
        }

        // Bottom action bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ActionButton(Icons.Default.SaveAlt, "Save", Color.White) { onSave(item) }
            ActionButton(Icons.Default.Share, "Share", Color.White) { onShare(item) }
            ActionButton(Icons.Default.Whatsapp, "Repost", Color(0xFF25D366)) { onRepost(item) }
        }
    }
}

@Composable
fun VideoPlayerContent(uri: Uri) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(Media3Item.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(icon, label, tint = tint, modifier = Modifier.size(28.dp))
        }
        Text(label, color = tint, style = MaterialTheme.typography.labelSmall)
    }
}
