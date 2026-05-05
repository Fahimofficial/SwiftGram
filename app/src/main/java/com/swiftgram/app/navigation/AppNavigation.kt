package com.swiftgram.app.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.swiftgram.app.ui.screens.AuthScreen
import com.swiftgram.app.ui.screens.ChatListScreen
import com.swiftgram.app.ui.viewmodels.AuthViewModel

/**
 * Navigation routes for the SwiftGram application.
 * 
 * Each route represents a distinct screen or flow in the app.
 * Routes are used to navigate between screens using the NavHostController.
 */
sealed class Route(val path: String) {
    /**
     * Authentication flow route.
     * Displays the authentication screens (phone, code, password, registration).
     */
    object Auth : Route("auth")
    
    /**
     * Chat list route.
     * Displays the list of all chats.
     */
    object ChatList : Route("chat_list")
    
    /**
     * Chat detail route.
     * Displays messages for a specific chat.
     * Parameter: chatId - The ID of the chat to display
     */
    object ChatDetail : Route("chat_detail/{chatId}") {
        fun createRoute(chatId: Long) = "chat_detail/$chatId"
    }
}

/**
 * Navigation host for the SwiftGram application.
 * 
 * Routes between authentication and main app screens based on authentication state.
 * The authentication screen handles the entire auth flow, and once complete,
 * the user is navigated to the chat list screen.
 *
 * @param navController The NavHostController managing navigation
 */
@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Route.Auth.path
    ) {
        // Authentication flow
        composable(Route.Auth.path) {
            val authViewModel: AuthViewModel = hiltViewModel()
            
            AuthScreen(
                viewModel = authViewModel,
                onAuthenticationComplete = {
                    // Navigate to chat list and remove auth from back stack
                    navController.navigate(Route.ChatList.path) {
                        popUpTo(Route.Auth.path) { inclusive = true }
                    }
                }
            )
        }
        
        // Chat list screen
        composable(Route.ChatList.path) {
            ChatListScreen()
        }
        
        // Chat detail screen
        composable(Route.ChatDetail.path) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId")?.toLongOrNull() ?: 0L
            // ChatDetailScreen(chatId = chatId)
            // TODO: Implement ChatDetailScreen
        }
    }
}
