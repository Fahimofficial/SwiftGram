package com.swiftgram.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.swiftgram.app.ui.viewmodels.AuthViewModel

/**
 * Screen for entering the phone number.
 * Displays input field and send button.
 *
 * @param viewModel The AuthViewModel managing authentication state
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
            modifier = Modifier.padding(bottom = 24.dp),
            color = MaterialTheme.colorScheme.onBackground
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
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )
        
        Button(
            onClick = { viewModel.sendPhoneNumber(phoneNumber) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = phoneNumber.isNotEmpty(),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Send Code")
        }
    }
}

/**
 * Screen for entering the verification code.
 * Displays code input field and verify button.
 *
 * @param viewModel The AuthViewModel managing authentication state
 * @param codeType The type of code sent (SMS, Telegram, etc.)
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
            modifier = Modifier.padding(bottom = 24.dp),
            color = MaterialTheme.colorScheme.onBackground
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
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )
        
        Button(
            onClick = { viewModel.verifyCode(authCode) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = authCode.isNotEmpty(),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Verify Code")
        }
    }
}

/**
 * Screen for entering the 2FA password.
 * Displays password input field with visibility toggle and verify button.
 *
 * @param viewModel The AuthViewModel managing authentication state
 */
@Composable
fun PasswordInputScreen(viewModel: AuthViewModel) {
    val password by viewModel.password.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Two-Factor Authentication",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp),
            color = MaterialTheme.colorScheme.onBackground
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
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )
        
        Button(
            onClick = { viewModel.verifyPassword(password) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = password.isNotEmpty(),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Verify Password")
        }
    }
}

/**
 * Screen for registering a new user account.
 * Displays first name and last name input fields with register button.
 *
 * @param viewModel The AuthViewModel managing authentication state
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
            modifier = Modifier.padding(bottom = 24.dp),
            color = MaterialTheme.colorScheme.onBackground
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
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )
        
        OutlinedTextField(
            value = lastName,
            onValueChange = { viewModel.updateLastName(it) },
            label = { Text("Last Name (Optional)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )
        
        Button(
            onClick = { viewModel.registerUser(firstName, lastName) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = firstName.isNotEmpty(),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Register")
        }
    }
}
