package com.pbhadoo.wassaver.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.pbhadoo.wassaver.viewmodel.UpdatesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesScreen(navController: NavController) {
    val vm: UpdatesViewModel = viewModel()
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { vm.checkForUpdates() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Check for Updates", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.checkForUpdates() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Checking GitHub releases...")
                }
            }
            state.error != null -> Box(Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Error, null, modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { vm.checkForUpdates() }) { Text("Retry") }
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // Current version card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (state.updateAvailable)
                                MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (state.updateAvailable) Icons.Default.SystemUpdate
                                else Icons.Default.CheckCircle,
                                null,
                                modifier = Modifier.size(32.dp),
                                tint = if (state.updateAvailable)
                                    MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (state.updateAvailable) "Update Available!" else "Up to Date",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Installed: ${state.currentVersion}  •  Latest: ${state.latestVersion ?: "—"}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                if (state.releases.isNotEmpty()) {
                    item {
                        Text("Release History", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                    }
                    items(state.releases) { release ->
                        val isCurrent = release.tagName == state.currentVersion
                        Card(shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(release.name.ifBlank { release.tagName },
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f))
                                    if (isCurrent) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text("INSTALLED",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Text(release.publishedAt,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (release.body.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(release.body.take(200) + if (release.body.length > 200) "…" else "",
                                        style = MaterialTheme.typography.bodySmall)
                                }
                                if (release.assets.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    release.assets.firstOrNull { it.name.endsWith(".apk") }?.let { asset ->
                                        OutlinedButton(
                                            onClick = {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_VIEW, Uri.parse(asset.downloadUrl))
                                                )
                                            }
                                        ) {
                                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Download APK (${asset.size / 1024 / 1024} MB)")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
