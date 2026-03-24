package com.pbhadoo.wassaver.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pbhadoo.wassaver.ui.navigation.Screen

data class HomeCard(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String,
    val section: String
)

val homeCards = listOf(
    HomeCard("Status Viewer", "View & save WhatsApp statuses", Icons.Outlined.Download, Screen.StatusViewer.route, "Status Tools"),
    HomeCard("Media Browser", "Browse private WA media", Icons.Outlined.PhotoLibrary, Screen.MediaBrowser.route, "Status Tools"),
    HomeCard("Saved Statuses", "All your saved statuses", Icons.Outlined.Bookmark, Screen.SavedStatuses.route, "Status Tools"),
    HomeCard("Status Splitter", "Split videos to 90s parts", Icons.Outlined.ContentCut, Screen.StatusSplitter.route, "Status Tools"),
    HomeCard("Deleted Messages", "Recover deleted messages", Icons.Outlined.RestoreFromTrash, Screen.DeletedMessages.route, "Sharing Tools"),
    HomeCard("Direct Message", "Message without saving contact", Icons.Outlined.Send, Screen.DirectMessage.route, "Sharing Tools"),
    HomeCard("Check for Updates", "Get the latest version", Icons.Outlined.SystemUpdate, Screen.Updates.route, "App"),
    HomeCard("About", "App info & credits", Icons.Outlined.Info, Screen.About.route, "App"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val sections = homeCards.groupBy { it.section }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "WASSaver",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "WhatsApp Status Manager",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Icon(
                        Icons.Default.Whatsapp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 16.dp).size(28.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sections.forEach { (sectionName, cards) ->
                item {
                    Text(
                        text = sectionName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }
                items(cards) { card ->
                    HomeFeatureCard(card = card, onClick = { navController.navigate(card.route) })
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun HomeFeatureCard(card: HomeCard, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = card.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    card.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    card.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
