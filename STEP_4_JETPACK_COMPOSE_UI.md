# Step 4: Jetpack Compose Authentication UI

This document provides complete implementation details for building the authentication flow using Jetpack Compose and Material 3 design system.

## Overview

**Objective:** Build a modern, responsive authentication UI that:
1. Displays authentication screens based on auth state
2. Handles user input for phone, code, password, and registration
3. Shows loading states and error messages
4. Follows Material 3 design guidelines
5. Provides smooth transitions between screens

**Architecture:**
- **AuthViewModel** – Manages authentication state and use cases
- **AuthScreen** – Main composable that routes to sub-screens
- **PhoneInputScreen** – Phone number input
- **CodeInputScreen** – Verification code input
- **PasswordInputScreen** – 2FA password input
- **RegistrationScreen** – New user registration
- **LoadingScreen** – Loading indicator
- **ErrorDialog** – Error message display

---

## Part 1: AuthViewModel

Create the ViewModel that manages authentication state and orchestrates use cases.

**File: `app/src/main/java/com/swiftgram/app/ui/viewmodels/AuthViewModel.kt`**

```kotlin
package com.swiftgram.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swiftgram.domain.common.Result
import com.swiftgram.domain.model.AuthState
import com.swiftgram.domain.usecase.auth.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing the authentication flow.
 * 
 * Responsibilities:
 * - Initialize Telegram client with API credentials
 * - Observe authentication state changes
 * - Handle user actions (send phone, code, password, register)
 * - Manage loading and error states
 * - Provide UI state as StateFlow for reactive updates
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val initializeTelegramUseCase: InitializeTelegramUseCase,
    private val sendPhoneNumberUseCase: SendPhoneNumberUseCase,
    private val verifyAuthenticationCodeUseCase: VerifyAuthenticationCodeUseCase,
    private val verifyPasswordUseCase: VerifyPasswordUseCase,
    private val registerUserUseCase: RegisterUserUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {
    
    private companion object {
        private const val TAG = "AuthViewModel"
    }
    
    // ==================== State Management ====================
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()
    
    private val _authCode = MutableStateFlow("")
    val authCode: StateFlow<String> = _authCode.asStateFlow()
    
    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()
    
    private val _firstName = MutableStateFlow("")
    val firstName: StateFlow<String> = _firstName.asStateFlow()
    
    private val _lastName = MutableStateFlow("")
    val lastName: StateFlow<String> = _lastName.asStateFlow()
    
    // ==================== Initialization ====================
    
    init {
        // Start observing auth state changes
        viewModelScope.launch {
            observeAuthStateUseCase().collect { state ->
                _authState.value = state
                // Clear loading state when auth state changes
                _isLoading.value = false
            }
        }
    }
    
    // ==================== Public Actions ====================
    
    /**
     * Initialize the Telegram client with API credentials.
     * Must be called before any other operations.
     *
     * @param apiId Telegram API ID
     * @param apiHash Telegram API Hash
     */
    fun initialize(apiId: Int, apiHash: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = initializeTelegramUseCase(
                InitializeTelegramUseCase.Params(apiId, apiHash)
            )
            
            when (result) {
                is Result.Success -> {
                    // Initialization successful, auth state updates will flow
                }
                is Result.Failure -> {
                    _errorMessage.value = result.exception.message ?: "Initialization failed"
                    _isLoading.value = false
                }
            }
        }
    }
    
    /**
     * Send the user's phone number for authentication.
     *
     * @param phoneNumber Phone number in format +1234567890
     */
    fun sendPhoneNumber(phoneNumber: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = sendPhoneNumberUseCase(
                SendPhoneNumberUseCase.Params(phoneNumber)
            )
            
            when (result) {
                is Result.Success -> {
                    _phoneNumber.value = phoneNumber
                    // Auth state will update to WaitingForCode
                }
                is Result.Failure -> {
                    _errorMessage.value = result.exception.message ?: "Failed to send phone number"
                    _isLoading.value = false
                }
            }
        }
    }
    
    /**
     * Verify the authentication code.
     *
     * @param code The verification code
     */
    fun verifyCode(code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = verifyAuthenticationCodeUseCase(
                VerifyAuthenticationCodeUseCase.Params(code)
            )
            
            when (result) {
                is Result.Success -> {
                    _authCode.value = code
                    // Auth state will update based on server response
                }
                is Result.Failure -> {
                    _errorMessage.value = result.exception.message ?: "Invalid code"
                    _isLoading.value = false
                }
            }
        }
    }
    
    /**
     * Verify the 2FA password.
     *
     * @param password The 2FA password
     */
    fun verifyPassword(password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = verifyPasswordUseCase(
                VerifyPasswordUseCase.Params(password)
            )
            
            when (result) {
                is Result.Success -> {
                    _password.value = password
                    // Auth state will update to Authenticated
                }
                is Result.Failure -> {
                    _errorMessage.value = result.exception.message ?: "Invalid password"
                    _isLoading.value = false
                }
            }
        }
    }
    
    /**
     * Register a new user account.
     *
     * @param firstName User's first name
     * @param lastName User's last name
     */
    fun registerUser(firstName: String, lastName: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = registerUserUseCase(
                RegisterUserUseCase.Params(firstName, lastName)
            )
            
            when (result) {
                is Result.Success -> {
                    _firstName.value = firstName
                    _lastName.value = lastName
                    // Auth state will update to Authenticated
                }
                is Result.Failure -> {
                    _errorMessage.value = result.exception.message ?: "Registration failed"
                    _isLoading.value = false
                }
            }
        }
    }
    
    /**
     * Logout the current user.
     */
    fun logout() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = logoutUseCase()
            
            when (result) {
                is Result.Success -> {
                    // Auth state will update to WaitingForPhoneNumber
                }
                is Result.Failure -> {
                    _errorMessage.value = result.exception.message ?: "Logout failed"
                    _isLoading.value = false
                }
            }
        }
    }
    
    /**
     * Update the phone number input field.
     */
    fun updatePhoneNumber(phone: String) {
        _phoneNumber.value = phone
    }
    
    /**
     * Update the auth code input field.
     */
    fun updateAuthCode(code: String) {
        _authCode.value = code
    }
    
    /**
     * Update the password input field.
     */
    fun updatePassword(password: String) {
        _password.value = password
    }
    
    /**
     * Update the first name input field.
     */
    fun updateFirstName(firstName: String) {
        _firstName.value = firstName
    }
    
    /**
     * Update the last name input field.
     */
    fun updateLastName(lastName: String) {
        _lastName.value = lastName
    }
    
    /**
     * Clear the error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }
}
```

---

## Part 2: Composable Screens

Create the Jetpack Compose screens for the authentication flow.

**File: `app/src/main/java/com/swiftgram/app/ui/screens/AuthScreen.kt`**

```kotlin
package com.swiftgram.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.swiftgram.app.ui.viewmodels.AuthViewModel
import com.swiftgram.domain.model.AuthState

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
 */
@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onAuthenticationComplete: () -> Unit = {}
) {
    val authState by viewModel.authState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    // Handle authentication completion
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
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
                // This shouldn't be shown as onAuthenticationComplete is called
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
 * Loading screen with a progress indicator.
 */
@Composable
fun LoadingScreen(message: String = "Loading...") {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
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
                contentAlignment = androidx.compose.ui.Alignment.Center
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
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
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
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

/**
 * Error dialog for displaying error messages.
 */
@Composable
fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Error") },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
```

---

## Part 3: Phone Input Screen

**File: `app/src/main/java/com/swiftgram/app/ui/screens/PhoneInputScreen.kt`**

```kotlin
package com.swiftgram.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.swiftgram.app.ui.viewmodels.AuthViewModel

/**
 * Screen for entering the phone number.
 * Displays input field and send button.
 */
@Composable
fun PhoneInputScreen(viewModel: AuthViewModel) {
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Enter Your Phone Number",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Text(
            text = "We'll send you a verification code via SMS or Telegram.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { viewModel.updatePhoneNumber(it) },
            label = { Text("Phone Number") },
            placeholder = { Text("+1234567890") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true
        )
        
        Button(
            onClick = { viewModel.sendPhoneNumber(phoneNumber) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = phoneNumber.isNotEmpty()
        ) {
            Text("Send Code")
        }
    }
}

/**
 * Screen for entering the verification code.
 */
@Composable
fun CodeInputScreen(viewModel: AuthViewModel, codeType: String) {
    val authCode by viewModel.authCode.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Enter Verification Code",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Text(
            text = "We sent a code via $codeType. Please enter it below.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        OutlinedTextField(
            value = authCode,
            onValueChange = { viewModel.updateAuthCode(it) },
            label = { Text("Verification Code") },
            placeholder = { Text("12345") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        
        Button(
            onClick = { viewModel.verifyCode(authCode) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = authCode.isNotEmpty()
        ) {
            Text("Verify Code")
        }
    }
}

/**
 * Screen for entering the 2FA password.
 */
@Composable
fun PasswordInputScreen(viewModel: AuthViewModel) {
    val password by viewModel.password.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Two-Factor Authentication",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Text(
            text = "Your account is protected with a password. Please enter it.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        OutlinedTextField(
            value = password,
            onValueChange = { viewModel.updatePassword(it) },
            label = { Text("Password") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true
        )
        
        Button(
            onClick = { viewModel.verifyPassword(password) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = password.isNotEmpty()
        ) {
            Text("Verify Password")
        }
    }
}

/**
 * Screen for registering a new user account.
 */
@Composable
fun RegistrationScreen(viewModel: AuthViewModel) {
    val firstName by viewModel.firstName.collectAsState()
    val lastName by viewModel.lastName.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Create Your Account",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Text(
            text = "This is your first time using Telegram. Please enter your name.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        OutlinedTextField(
            value = firstName,
            onValueChange = { viewModel.updateFirstName(it) },
            label = { Text("First Name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            singleLine = true
        )
        
        OutlinedTextField(
            value = lastName,
            onValueChange = { viewModel.updateLastName(it) },
            label = { Text("Last Name (Optional)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true
        )
        
        Button(
            onClick = { viewModel.registerUser(firstName, lastName) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = firstName.isNotEmpty()
        ) {
            Text("Register")
        }
    }
}
```

---

## Part 4: Update MainActivity

Update the MainActivity to initialize the ViewModel and pass API credentials.

**File: `app/src/main/java/com/swiftgram/app/MainActivity.kt`**

```kotlin
package com.swiftgram.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.swiftgram.app.ui.screens.AuthScreen
import com.swiftgram.app.ui.theme.SwiftGramTheme
import com.swiftgram.app.ui.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for the SwiftGram application.
 * Displays the authentication flow.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            SwiftGramTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authViewModel: AuthViewModel = hiltViewModel()
                    
                    // Initialize Telegram client on first launch
                    // In production, get these from BuildConfig or secure storage
                    val apiId = 123456  // Replace with actual API ID
                    val apiHash = "your_api_hash_here"  // Replace with actual API Hash
                    
                    AuthScreen(
                        viewModel = authViewModel,
                        onAuthenticationComplete = {
                            // Navigate to main app screen
                            // For now, just log it
                            println("Authentication complete!")
                        }
                    )
                    
                    // Initialize on first composition
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        authViewModel.initialize(apiId, apiHash)
                    }
                }
            }
        }
    }
}
```

---

## Part 5: Update App Navigation

Create a navigation structure for the app.

**File: `app/src/main/java/com/swiftgram/app/navigation/AppNavigation.kt`**

```kotlin
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
 * Navigation routes for the app.
 */
sealed class Route(val path: String) {
    object Auth : Route("auth")
    object ChatList : Route("chat_list")
    object ChatDetail : Route("chat_detail/{chatId}")
}

/**
 * Navigation host for the app.
 * Routes between authentication and main app screens.
 */
@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Route.Auth.path
    ) {
        composable(Route.Auth.path) {
            val authViewModel: AuthViewModel = hiltViewModel()
            
            AuthScreen(
                viewModel = authViewModel,
                onAuthenticationComplete = {
                    navController.navigate(Route.ChatList.path) {
                        popUpTo(Route.Auth.path) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Route.ChatList.path) {
            ChatListScreen()
        }
    }
}
```

---

## Part 6: Material 3 Theme

Update the theme to use Material 3 and SwiftGram branding.

**File: `app/src/main/java/com/swiftgram/app/ui/theme/Theme.kt`**

```kotlin
package com.swiftgram.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import android.content.Context

/**
 * SwiftGram color palette.
 * Primary: Telegram blue
 * Secondary: Accent color
 * Tertiary: Additional accent
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF0088CC),           // Telegram blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E8FF),
    onPrimaryContainer = Color(0xFF001F5C),
    
    secondary = Color(0xFF0088CC),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E8FF),
    onSecondaryContainer = Color(0xFF001F5C),
    
    tertiary = Color(0xFF00A699),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFA0F0E8),
    onTertiaryContainer = Color(0xFF002019),
    
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1A1A1A),
    
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),
    
    error = Color(0xFFB3261E),
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5DADE2),           // Light Telegram blue
    onPrimary = Color(0xFF001F5C),
    primaryContainer = Color(0xFF003A8A),
    onPrimaryContainer = Color(0xFFD6E8FF),
    
    secondary = Color(0xFF5DADE2),
    onSecondary = Color(0xFF001F5C),
    secondaryContainer = Color(0xFF003A8A),
    onSecondaryContainer = Color(0xFFD6E8FF),
    
    tertiary = Color(0xFF83D8CE),
    onTertiary = Color(0xFF003A33),
    tertiaryContainer = Color(0xFF00524A),
    onTertiaryContainer = Color(0xFFA0F0E8),
    
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    
    error = Color(0xFFF9DEDC),
    onError = Color(0xFF601410)
)

/**
 * SwiftGram theme composable.
 * Supports light, dark, and dynamic theming based on device capabilities.
 */
@Composable
fun SwiftGramTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            // Use dynamic theming on Android 12+
            val context = androidx.compose.ui.platform.LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = SwiftGramTypography,
        shapes = SwiftGramShapes,
        content = content
    )
}
```

---

## Part 7: Folder Structure Summary

After completing Step 4, your project structure should look like:

```
app/src/main/java/com/swiftgram/app/
├── MainActivity.kt
├── di/
│   └── AppModule.kt
├── navigation/
│   └── AppNavigation.kt
├── ui/
│   ├── screens/
│   │   ├── AuthScreen.kt
│   │   ├── PhoneInputScreen.kt
│   │   ├── CodeInputScreen.kt
│   │   ├── PasswordInputScreen.kt
│   │   ├── RegistrationScreen.kt
│   │   ├── ChatListScreen.kt
│   │   └── ChatDetailScreen.kt
│   ├── theme/
│   │   ├── Theme.kt
│   │   ├── Color.kt
│   │   ├── Type.kt
│   │   └── Shape.kt
│   ├── components/
│   │   ├── ChatListItem.kt
│   │   ├── MessageBubble.kt
│   │   └── InputField.kt
│   └── viewmodels/
│       ├── AuthViewModel.kt
│       ├── ChatListViewModel.kt
│       └── ChatDetailViewModel.kt
```

---

## Key Design Principles

**Reactive UI:** StateFlow-based state management for reactive UI updates.

**Separation of Concerns:** ViewModel handles business logic, Composables handle UI.

**Material 3:** Modern Material Design system with dynamic theming support.

**Error Handling:** Proper error messages and retry mechanisms.

**Loading States:** Clear loading indicators during async operations.

**Accessibility:** Proper labels and keyboard support for all inputs.

---

## Next Steps

Once Step 4 is complete:
- Test the authentication flow end-to-end
- Verify all screens render correctly
- Test error handling and loading states
- Build and run on an Android device

The SwiftGram authentication flow is now complete and ready for integration with TDLib!

