package com.swiftgram.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.swiftgram.app.ui.viewmodels.AuthViewModel
import com.swiftgram.domain.model.AuthState

/**
 * Login screen that handles the entire authentication flow.
 * Routes to different UI states based on the current AuthState.
 *
 * @param viewModel The authentication ViewModel
 * @param onAuthenticationComplete Callback when authentication is successful
 */
@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onAuthenticationComplete: () -> Unit = {}
) {
    val authState by viewModel.authState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    // Handle successful authentication
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
        when (authState) {
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is AuthState.Error -> {
                ErrorScreen(
                    message = (authState as AuthState.Error).message,
                    onRetry = { viewModel.clearError() }
                )
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        
        // Loading overlay
        if (isLoading) {
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
        
        // Error dialog
        if (errorMessage != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text("Error") },
                text = { Text(errorMessage!!) },
                confirmButton = {
                    Button(onClick = { viewModel.clearError() }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

/**
 * Screen for entering the phone number.
 */
@Composable
fun PhoneInputScreen(viewModel: AuthViewModel) {
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Enter Your Phone Number",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "We'll send you a verification code via SMS or Telegram.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { viewModel.updatePhoneNumber(it) },
            label = { Text("Phone Number") },
            placeholder = { Text("+1234567890") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
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
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Enter Verification Code",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "We sent a code via $codeType. Please enter it below.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        OutlinedTextField(
            value = authCode,
            onValueChange = { viewModel.updateAuthCode(it) },
            label = { Text("Verification Code") },
            placeholder = { Text("12345") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
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
    var passwordVisible by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Two-Factor Authentication",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "Your account is protected with a password. Please enter it.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        OutlinedTextField(
            value = password,
            onValueChange = { viewModel.updatePassword(it) },
            label = { Text("Password") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
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
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Create Your Account",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "This is your first time using Telegram. Please enter your name.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 24.dp),
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
                .padding(bottom = 24.dp),
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

/**
 * Error screen displayed when an error occurs.
 */
@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Error",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}
