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
import timber.log.Timber
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
 *
 * This ViewModel uses Hilt for dependency injection and provides
 * a clean interface for the authentication screens.
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
                _isLoading.value = false
                Timber.tag(TAG).d("Auth state changed: $state")
            }
        }
    }
    
    // ==================== Public Actions ====================
    
    /**
     * Initialize the Telegram client with API credentials.
     * Must be called before any other operations.
     *
     * @param apiId Telegram API ID from BuildConfig
     * @param apiHash Telegram API Hash from BuildConfig
     */
    fun initialize(apiId: Int, apiHash: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            Timber.tag(TAG).d("Initializing Telegram client")
            
            val result = initializeTelegramUseCase(
                InitializeTelegramUseCase.Params(apiId, apiHash)
            )
            
            when (result) {
                is Result.Success -> {
                    Timber.tag(TAG).d("Telegram client initialized successfully")
                }
                is Result.Failure -> {
                    val errorMsg = result.exception.message ?: "Initialization failed"
                    _errorMessage.value = errorMsg
                    _isLoading.value = false
                    Timber.tag(TAG).e(result.exception, "Initialization failed")
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
            
            Timber.tag(TAG).d("Sending phone number: $phoneNumber")
            
            val result = sendPhoneNumberUseCase(
                SendPhoneNumberUseCase.Params(phoneNumber)
            )
            
            when (result) {
                is Result.Success -> {
                    _phoneNumber.value = phoneNumber
                    Timber.tag(TAG).d("Phone number sent successfully")
                }
                is Result.Failure -> {
                    val errorMsg = result.exception.message ?: "Failed to send phone number"
                    _errorMessage.value = errorMsg
                    _isLoading.value = false
                    Timber.tag(TAG).e(result.exception, "Failed to send phone number")
                }
            }
        }
    }
    
    /**
     * Verify the authentication code.
     *
     * @param code The verification code (typically 5 digits)
     */
    fun verifyCode(code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            Timber.tag(TAG).d("Verifying authentication code")
            
            val result = verifyAuthenticationCodeUseCase(
                VerifyAuthenticationCodeUseCase.Params(code)
            )
            
            when (result) {
                is Result.Success -> {
                    _authCode.value = code
                    Timber.tag(TAG).d("Authentication code verified successfully")
                }
                is Result.Failure -> {
                    val errorMsg = result.exception.message ?: "Invalid code"
                    _errorMessage.value = errorMsg
                    _isLoading.value = false
                    Timber.tag(TAG).e(result.exception, "Failed to verify code")
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
            
            Timber.tag(TAG).d("Verifying 2FA password")
            
            val result = verifyPasswordUseCase(
                VerifyPasswordUseCase.Params(password)
            )
            
            when (result) {
                is Result.Success -> {
                    _password.value = password
                    Timber.tag(TAG).d("2FA password verified successfully")
                }
                is Result.Failure -> {
                    val errorMsg = result.exception.message ?: "Invalid password"
                    _errorMessage.value = errorMsg
                    _isLoading.value = false
                    Timber.tag(TAG).e(result.exception, "Failed to verify password")
                }
            }
        }
    }
    
    /**
     * Register a new user account.
     *
     * @param firstName User's first name (required)
     * @param lastName User's last name (optional)
     */
    fun registerUser(firstName: String, lastName: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            Timber.tag(TAG).d("Registering new user: $firstName $lastName")
            
            val result = registerUserUseCase(
                RegisterUserUseCase.Params(firstName, lastName)
            )
            
            when (result) {
                is Result.Success -> {
                    _firstName.value = firstName
                    _lastName.value = lastName
                    Timber.tag(TAG).d("User registered successfully")
                }
                is Result.Failure -> {
                    val errorMsg = result.exception.message ?: "Registration failed"
                    _errorMessage.value = errorMsg
                    _isLoading.value = false
                    Timber.tag(TAG).e(result.exception, "Registration failed")
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
            
            Timber.tag(TAG).d("Logging out")
            
            val result = logoutUseCase()
            
            when (result) {
                is Result.Success -> {
                    Timber.tag(TAG).d("Logged out successfully")
                }
                is Result.Failure -> {
                    val errorMsg = result.exception.message ?: "Logout failed"
                    _errorMessage.value = errorMsg
                    _isLoading.value = false
                    Timber.tag(TAG).e(result.exception, "Logout failed")
                }
            }
        }
    }
    
    // ==================== Input Field Updates ====================
    
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
