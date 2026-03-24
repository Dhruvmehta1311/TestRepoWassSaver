package com.pbhadoo.wassaver.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pbhadoo.wassaver.ui.screens.*

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object StatusViewer : Screen("status_viewer")
    object MediaBrowser : Screen("media_browser")
    object SavedStatuses : Screen("saved_statuses")
    object StatusSplitter : Screen("status_splitter")
    object DeletedMessages : Screen("deleted_messages")
    object DeletedMessageChat : Screen("deleted_message_chat/{senderId}") {
        fun createRoute(senderId: String) = "deleted_message_chat/$senderId"
    }
    object DirectMessage : Screen("direct_message")
    object Updates : Screen("updates")
    object About : Screen("about")
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }
        composable(Screen.StatusViewer.route) {
            StatusViewerScreen(navController)
        }
        composable(Screen.MediaBrowser.route) {
            MediaBrowserScreen(navController)
        }
        composable(Screen.SavedStatuses.route) {
            SavedStatusesScreen(navController)
        }
        composable(Screen.StatusSplitter.route) {
            StatusSplitterScreen(navController)
        }
        composable(Screen.DeletedMessages.route) {
            DeletedMessagesScreen(navController)
        }
        composable(Screen.DeletedMessageChat.route) { backStackEntry ->
            val senderId = backStackEntry.arguments?.getString("senderId") ?: ""
            DeletedMessageChatScreen(navController, senderId)
        }
        composable(Screen.DirectMessage.route) {
            DirectMessageScreen(navController)
        }
        composable(Screen.Updates.route) {
            UpdatesScreen(navController)
        }
        composable(Screen.About.route) {
            AboutScreen(navController)
        }
    }
}
