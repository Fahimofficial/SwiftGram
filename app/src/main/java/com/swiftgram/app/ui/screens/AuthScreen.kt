package com.swiftgram.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.swiftgram.app.ui.viewmodels.AuthViewModel
import com.swiftgram.domain.model.AuthState
import timber.log.Timber

/**
 * Main authentication screen that routes to sub-screens based on auth state.
 * 
 * This composable observes the auth state and displays the appropriate screen:
 * - Loading screen during initialization
 * - Phone input screen when waiting for phone number
 * - Code input screen when waiting for verification code
 * - Password input screen when 2FA is required
 * - Registration screen for new users
 * - Error dialog when an error occurs
 *
 * @param viewModel The AuthViewModel managing authentication state
 * @param onAuthenticationComplete Callback when authentication is successful
 */
@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onAuthenticationComplete: () -> Unit = {}
) {
    val authState by viewModel.authState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    Timber.d("AuthScreen: Current auth state = $authState, isLoading = $isLoading")
    
    // Handle authentication completion
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            Timber.d("Authentication complete, calling callback")
            onAuthenticationComplete()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        // Display the appropriate screen based on auth state
        when (authState) {
            is AuthState.WaitingForTDLibParameters,
            is AuthState.WaitingForEncryptionKey -> {
                LoadingScreen(message = "Initializing...")
            }
            
            is AuthState.WaitingForPhoneNumber -> {
                PhoneInputScreen(viewModel)
            }
            
            is AuthState.WaitingForCode -> {
                val codeType = (authState as AuthState.WaitingForCode).codeType
                CodeInputScreen(viewModel, codeType)
            }
            
            is AuthState.WaitingForPassword -> {
                PasswordInputScreen(viewModel)
            }
            
            is AuthState.WaitingForRegistration -> {
                RegistrationScreen(viewModel)
            }
            
            is AuthState.Authenticated -> {
                LoadingScreen(message = "Authenticated!")
            }
            
            is AuthState.LoggingOut,
            is AuthState.Closing -> {
                LoadingScreen(message = "Logging out...")
            }
            
            is AuthState.Error -> {
                ErrorScreen(
                    message = (authState as AuthState.Error).message,
                    onRetry = { viewModel.clearError() }
                )
            }
            
            else -> {
                LoadingScreen(message = "Loading...")
            }
        }
        
        // Show loading overlay
        if (isLoading) {
            LoadingOverlay()
        }
        
        // Show error dialog
        if (errorMessage != null) {
            ErrorDialog(
                message = errorMessage!!,
                onDismiss = { viewModel.clearError() }
            )
        }
    }
}

/**
 * Loading screen with a progress indicator and message.
 *
 * @param message The message to display
 */
@Composable
fun LoadingScreen(message: String = "Loading...") {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

/**
 * Loading overlay that appears on top of other content.
 * Shows a semi-transparent background with a progress indicator.
 */
@Composable
fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Error screen displayed when an error occurs.
 *
 * @param message The error message to display
 * @param onRetry Callback when the retry button is clicked
 */
@Composable
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Error",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

/**
 * Error dialog for displaying error messages.
 * Appears as a modal dialog on top of the current screen.
 *
 * @param message The error message to display
 * @param onDismiss Callback when the dialog is dismissed
 */
@Composable
fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Error",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = { 
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
