package com.pbhadoo.wassaver.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.pbhadoo.wassaver.viewmodel.StatusSplitterViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusSplitterScreen(navController: NavController) {
    val vm: StatusSplitterViewModel = viewModel()
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(idx)
            } ?: "video.mp4"
            vm.onVideoSelected(uri, name)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Status Splitter", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Pick video card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Select Video", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Pick any video to split into 90-second WhatsApp-ready parts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { videoPickerLauncher.launch("video/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.VideoLibrary, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Pick Video")
                        }
                    }
                }
            }

            // Video info
            if (state.selectedUri != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Video Info", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            InfoRow("File", state.selectedName ?: "unknown")
                            InfoRow("Duration", formatDuration(state.durationMs))
                            InfoRow("Estimated Parts", "${state.estimatedParts} × 90-second clips")
                            Spacer(Modifier.height(16.dp))
                            if (state.isSplitting) {
                                LinearProgressIndicator(
                                    progress = { state.progress },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("Splitting... ${(state.progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall)
                            } else {
                                Button(
                                    onClick = { vm.splitVideo() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.ContentCut, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Split Video")
                                }
                            }
                        }
                    }
                }
            }

            // Output files
            if (state.outputFiles.isNotEmpty()) {
                item {
                    Text(
                        "Split Parts (${state.outputFiles.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                itemsIndexed(state.outputFiles) { index, file ->
                    SplitPartCard(
                        index = index + 1,
                        file = file,
                        onShare = { vm.sharePartToWhatsApp(context, file) }
                    )
                }
            }

            // Message
            state.message?.let { msg ->
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(msg, modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SplitPartCard(index: Int, file: File, onShare: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("$index", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(file.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text("${file.length() / 1024 / 1024} MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Whatsapp, "Share to WhatsApp",
                    tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, modifier = Modifier.width(120.dp), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    val m = seconds / 60
    val s = seconds % 60
    return "${m}m ${s}s"
}
