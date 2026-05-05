# Step 2: TDLib Client Wrapper

This document provides complete implementation details for creating a production-grade TDLib client wrapper that handles Telegram API communication, initialization, and async event processing.

## Overview

**Objective:** Build a singleton TDLib client wrapper that:
1. Initializes TDLib with proper configuration and credentials
2. Manages the connection lifecycle (connect, authenticate, disconnect)
3. Listens to TDLib async updates and exposes them as Kotlin `Flow`
4. Provides a clean, coroutine-based API for the rest of the application

**Architecture:**
- **TelegramClient** – Singleton wrapper around TDLib's `Client` class
- **TelegramEventListener** – Coroutine-based event loop that collects TDLib updates
- **TelegramUpdate** – Sealed class representing all possible TDLib events
- **TelegramRepository** – Data layer interface for Telegram operations

---

## Part 1: TDLib Update Models

Create a sealed class to represent all TDLib events in a type-safe manner.

**File: `data/src/main/java/com/swiftgram/data/remote/telegram/TelegramUpdate.kt`**

```kotlin
package com.swiftgram.data.remote.telegram

import org.drinkless.tdlib.TdApi

/**
 * Sealed class representing all possible TDLib updates.
 * Provides type-safe handling of Telegram events using Kotlin sealed classes.
 */
sealed class TelegramUpdate {
    
    // Authentication Updates
    data class AuthorizationStateUpdate(val state: TdApi.AuthorizationState) : TelegramUpdate()
    
    // Connection Updates
    data class ConnectionStateUpdate(val state: TdApi.ConnectionState) : TelegramUpdate()
    
    // Chat Updates
    data class ChatUpdate(val chat: TdApi.Chat) : TelegramUpdate()
    data class ChatListUpdate(val chatList: TdApi.ChatList, val chatIds: LongArray) : TelegramUpdate()
    
    // Message Updates
    data class NewMessageUpdate(val message: TdApi.Message) : TelegramUpdate()
    data class MessageUpdate(val message: TdApi.Message) : TelegramUpdate()
    data class MessageDeletedUpdate(val chatId: Long, val messageIds: LongArray) : TelegramUpdate()
    
    // User Updates
    data class UserUpdate(val user: TdApi.User) : TelegramUpdate()
    data class UserStatusUpdate(val userId: Long, val status: TdApi.UserStatus) : TelegramUpdate()
    
    // Generic Updates
    data class GenericUpdate(val update: TdApi.Object) : TelegramUpdate()
    
    // Error Updates
    data class ErrorUpdate(val error: TdApi.Error) : TelegramUpdate()
}
```

---

## Part 2: TDLib Client Wrapper

Create the main singleton client that wraps TDLib's `Client` class.

**File: `data/src/main/java/com/swiftgram/data/remote/telegram/TelegramClient.kt`**

```kotlin
package com.swiftgram.data.remote.telegram

import android.content.Context
import com.swiftgram.core.utils.Logger
import com.swiftgram.core.utils.TDLibLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton wrapper around TDLib's Client.
 * Manages TDLib initialization, connection lifecycle, and event handling.
 *
 * Usage:
 * ```
 * val client = TelegramClient.getInstance()
 * client.initialize(context, apiId, apiHash)
 * client.updates.collect { update ->
 *     when (update) {
 *         is TelegramUpdate.AuthorizationStateUpdate -> { /* handle auth */ }
 *         is TelegramUpdate.NewMessageUpdate -> { /* handle message */ }
 *         else -> {}
 *     }
 * }
 * ```
 */
object TelegramClient {
    private const val TAG = "TelegramClient"
    private const val DATABASE_DIR = "tdlib"
    private const val FILES_DIR = "tdlib_files"
    
    private var client: Client? = null
    private var isInitialized = false
    private var isConnected = false
    
    private val _updates = MutableSharedFlow<TelegramUpdate>(
        replay = 0,
        extraBufferCapacity = 100
    )
    val updates: SharedFlow<TelegramUpdate> = _updates.asSharedFlow()
    
    private val requestCallbacks = ConcurrentHashMap<Long, (TdApi.Object) -> Unit>()
    private var nextRequestId = 1L
    
    /**
     * Initialize TDLib with the provided configuration.
     * Must be called once before any other operations.
     *
     * @param context Android context for accessing app directories
     * @param apiId Telegram API ID from https://my.telegram.org/apps
     * @param apiHash Telegram API Hash from https://my.telegram.org/apps
     * @param phoneNumber Optional phone number for auto-login (for testing)
     */
    suspend fun initialize(
        context: Context,
        apiId: Int,
        apiHash: String,
        phoneNumber: String? = null
    ) = withContext(Dispatchers.Default) {
        if (isInitialized) {
            Logger.d(TAG, "TelegramClient already initialized")
            return@withContext
        }
        
        try {
            // Load native TDLib libraries
            TDLibLoader.loadLibraries(context)
            Logger.d(TAG, "TDLib native libraries loaded")
            
            // Set up logging (optional, but useful for debugging)
            Client.setLogVerbosityLevel(0)  // 0 = errors only, 1 = warnings, 2 = info, 3 = debug
            
            // Create the TDLib client
            client = Client.create(
                { update -> handleTdLibUpdate(update) },
                { update -> handleTdLibUpdate(update) },
                { update -> handleTdLibUpdate(update) }
            )
            Logger.d(TAG, "TDLib Client created")
            
            // Configure TDLib
            val dbDir = context.getDir(DATABASE_DIR, Context.MODE_PRIVATE).absolutePath
            val filesDir = context.getDir(FILES_DIR, Context.MODE_PRIVATE).absolutePath
            
            val setTdlibParameters = TdApi.SetTdlibParameters()
            setTdlibParameters.databaseDirectory = dbDir
            setTdlibParameters.filesDirectory = filesDir
            setTdlibParameters.useFileDatabase = true
            setTdlibParameters.useChatInfoDatabase = true
            setTdlibParameters.useMessageDatabase = true
            setTdlibParameters.useSecretChats = true
            setTdlibParameters.apiId = apiId
            setTdlibParameters.apiHash = apiHash
            setTdlibParameters.systemLanguageCode = "en"
            setTdlibParameters.deviceModel = android.os.Build.MODEL
            setTdlibParameters.systemVersion = android.os.Build.VERSION.RELEASE
            setTdlibParameters.applicationVersion = "1.0.0"
            setTdlibParameters.enableStorageOptimizer = true
            setTdlibParameters.useSecondaryServerForSecretChats = true
            
            sendRequest(setTdlibParameters)
            Logger.d(TAG, "TDLib parameters configured")
            
            // Encrypt the database with a passphrase (optional but recommended)
            val encryptionKey = TdApi.SetDatabaseEncryptionKey()
            encryptionKey.newEncryptionKey = ByteArray(0)  // Empty key for no encryption (use proper key in production)
            sendRequest(encryptionKey)
            Logger.d(TAG, "Database encryption configured")
            
            isInitialized = true
            Logger.i(TAG, "TelegramClient initialized successfully")
            
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to initialize TelegramClient", e)
            throw RuntimeException("TelegramClient initialization failed: ${e.message}", e)
        }
    }
    
    /**
     * Connect to Telegram servers.
     * Must be called after initialization.
     */
    suspend fun connect() = withContext(Dispatchers.Default) {
        if (!isInitialized) {
            throw IllegalStateException("TelegramClient not initialized. Call initialize() first.")
        }
        
        if (isConnected) {
            Logger.d(TAG, "Already connected")
            return@withContext
        }
        
        try {
            sendRequest(TdApi.GetAuthorizationState())
            isConnected = true
            Logger.i(TAG, "Connected to Telegram")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to connect", e)
            throw e
        }
    }
    
    /**
     * Disconnect from Telegram servers.
     */
    suspend fun disconnect() = withContext(Dispatchers.Default) {
        if (!isConnected) {
            Logger.d(TAG, "Already disconnected")
            return@withContext
        }
        
        try {
            sendRequest(TdApi.Close())
            isConnected = false
            Logger.i(TAG, "Disconnected from Telegram")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to disconnect", e)
        }
    }
    
    /**
     * Send a request to TDLib and get the response via callback.
     *
     * @param function The TDLib request function
     * @param callback Optional callback to receive the response
     */
    fun sendRequest(
        function: TdApi.Function,
        callback: ((TdApi.Object) -> Unit)? = null
    ): Long {
        if (!isInitialized) {
            throw IllegalStateException("TelegramClient not initialized")
        }
        
        val requestId = nextRequestId++
        
        if (callback != null) {
            requestCallbacks[requestId] = callback
        }
        
        client?.send(function, requestId) { response ->
            callback?.invoke(response)
            requestCallbacks.remove(requestId)
        }
        
        return requestId
    }
    
    /**
     * Send a request and wait for the response (blocking).
     * Use with caution; prefer async sendRequest() for better performance.
     *
     * @param function The TDLib request function
     * @param timeoutMs Timeout in milliseconds
     * @return The response from TDLib
     */
    suspend fun sendRequestSync(
        function: TdApi.Function,
        timeoutMs: Long = 5000
    ): TdApi.Object = withContext(Dispatchers.Default) {
        var response: TdApi.Object? = null
        val lock = Object()
        
        sendRequest(function) { result ->
            synchronized(lock) {
                response = result
                lock.notifyAll()
            }
        }
        
        synchronized(lock) {
            if (response == null) {
                lock.wait(timeoutMs)
            }
        }
        
        response ?: throw TimeoutException("TDLib request timed out after ${timeoutMs}ms")
    }
    
    /**
     * Handle incoming TDLib updates and emit them as TelegramUpdate events.
     */
    private fun handleTdLibUpdate(update: TdApi.Object) {
        try {
            val telegramUpdate = when (update) {
                is TdApi.UpdateAuthorizationState -> {
                    TelegramUpdate.AuthorizationStateUpdate(update.authorizationState)
                }
                is TdApi.UpdateConnectionState -> {
                    TelegramUpdate.ConnectionStateUpdate(update.connectionState)
                }
                is TdApi.UpdateChat -> {
                    TelegramUpdate.ChatUpdate(update.chat)
                }
                is TdApi.UpdateChatList -> {
                    TelegramUpdate.ChatListUpdate(update.chatList, update.chatIds)
                }
                is TdApi.UpdateNewMessage -> {
                    TelegramUpdate.NewMessageUpdate(update.message)
                }
                is TdApi.UpdateMessageContent -> {
                    // Extract message from update if available
                    TelegramUpdate.GenericUpdate(update)
                }
                is TdApi.UpdateDeleteMessages -> {
                    TelegramUpdate.MessageDeletedUpdate(update.chatId, update.messageIds)
                }
                is TdApi.UpdateUser -> {
                    TelegramUpdate.UserUpdate(update.user)
                }
                is TdApi.UpdateUserStatus -> {
                    TelegramUpdate.UserStatusUpdate(update.userId, update.status)
                }
                is TdApi.Error -> {
                    TelegramUpdate.ErrorUpdate(update)
                }
                else -> {
                    TelegramUpdate.GenericUpdate(update)
                }
            }
            
            // Emit the update to all collectors
            _updates.tryEmit(telegramUpdate)
            
        } catch (e: Exception) {
            Logger.e(TAG, "Error handling TDLib update", e)
        }
    }
    
    /**
     * Get the current authorization state.
     */
    suspend fun getAuthorizationState(): TdApi.AuthorizationState? = withContext(Dispatchers.Default) {
        var state: TdApi.AuthorizationState? = null
        val lock = Object()
        
        sendRequest(TdApi.GetAuthorizationState()) { response ->
            synchronized(lock) {
                if (response is TdApi.AuthorizationState) {
                    state = response
                }
                lock.notifyAll()
            }
        }
        
        synchronized(lock) {
            lock.wait(2000)
        }
        
        state
    }
    
    /**
     * Check if the client is initialized.
     */
    fun isInitialized(): Boolean = isInitialized
    
    /**
     * Check if the client is connected.
     */
    fun isConnected(): Boolean = isConnected
    
    /**
     * Reset the client state (useful for testing or logout).
     */
    fun reset() {
        client = null
        isInitialized = false
        isConnected = false
        requestCallbacks.clear()
        nextRequestId = 1L
        Logger.d(TAG, "TelegramClient reset")
    }
}
```

---

## Part 3: TelegramRepository Interface

Create a repository interface in the `domain` module to define the contract for Telegram operations.

**File: `domain/src/main/java/com/swiftgram/domain/repository/TelegramRepository.kt`**

```kotlin
package com.swiftgram.domain.repository

import com.swiftgram.domain.model.AuthState
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Telegram operations.
 * Defines the contract for authentication, messaging, and chat operations.
 */
interface TelegramRepository {
    
    /**
     * Initialize the Telegram client with API credentials.
     */
    suspend fun initialize(apiId: Int, apiHash: String)
    
    /**
     * Connect to Telegram servers.
     */
    suspend fun connect()
    
    /**
     * Disconnect from Telegram servers.
     */
    suspend fun disconnect()
    
    /**
     * Get the current authorization state as a Flow.
     * Emits updates whenever the auth state changes.
     */
    fun observeAuthorizationState(): Flow<AuthState>
    
    /**
     * Send a phone number for authentication.
     */
    suspend fun sendPhoneNumber(phoneNumber: String)
    
    /**
     * Send the authentication code received via SMS/Telegram.
     */
    suspend fun sendAuthenticationCode(code: String)
    
    /**
     * Send the password for 2FA authentication.
     */
    suspend fun sendPassword(password: String)
    
    /**
     * Register a new account (for new users).
     */
    suspend fun registerUser(firstName: String, lastName: String = "")
    
    /**
     * Log out from the current account.
     */
    suspend fun logout()
}
```

---

## Part 4: TelegramRepository Implementation

Create the implementation in the `data` module.

**File: `data/src/main/java/com/swiftgram/data/repository/TelegramRepositoryImpl.kt`**

```kotlin
package com.swiftgram.data.repository

import android.content.Context
import com.swiftgram.core.utils.Logger
import com.swiftgram.data.remote.telegram.TelegramClient
import com.swiftgram.data.remote.telegram.TelegramUpdate
import com.swiftgram.domain.model.AuthState
import com.swiftgram.domain.repository.TelegramRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.drinkless.tdlib.TdApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TelegramRepository using TelegramClient.
 * Handles all Telegram API operations and state management.
 */
@Singleton
class TelegramRepositoryImpl @Inject constructor(
    private val context: Context
) : TelegramRepository {
    
    private companion object {
        private const val TAG = "TelegramRepositoryImpl"
    }
    
    override suspend fun initialize(apiId: Int, apiHash: String) {
        Logger.d(TAG, "Initializing Telegram client")
        TelegramClient.initialize(context, apiId, apiHash)
    }
    
    override suspend fun connect() {
        Logger.d(TAG, "Connecting to Telegram")
        TelegramClient.connect()
    }
    
    override suspend fun disconnect() {
        Logger.d(TAG, "Disconnecting from Telegram")
        TelegramClient.disconnect()
    }
    
    override fun observeAuthorizationState(): Flow<AuthState> {
        return TelegramClient.updates
            .map { update ->
                when (update) {
                    is TelegramUpdate.AuthorizationStateUpdate -> {
                        mapAuthorizationState(update.state)
                    }
                    is TelegramUpdate.ErrorUpdate -> {
                        AuthState.Error(update.error.message)
                    }
                    else -> AuthState.Unknown
                }
            }
    }
    
    override suspend fun sendPhoneNumber(phoneNumber: String) {
        Logger.d(TAG, "Sending phone number: $phoneNumber")
        val request = TdApi.SetAuthenticationPhoneNumber()
        request.phoneNumber = phoneNumber
        request.settings = TdApi.PhoneNumberAuthenticationSettings()
        request.settings.allowFlashCall = false
        request.settings.allowMissedCall = false
        
        TelegramClient.sendRequest(request)
    }
    
    override suspend fun sendAuthenticationCode(code: String) {
        Logger.d(TAG, "Sending authentication code")
        val request = TdApi.CheckAuthenticationCode()
        request.code = code
        
        TelegramClient.sendRequest(request)
    }
    
    override suspend fun sendPassword(password: String) {
        Logger.d(TAG, "Sending 2FA password")
        val request = TdApi.CheckAuthenticationPassword()
        request.password = password
        
        TelegramClient.sendRequest(request)
    }
    
    override suspend fun registerUser(firstName: String, lastName: String) {
        Logger.d(TAG, "Registering new user: $firstName $lastName")
        val request = TdApi.RegisterUser()
        request.firstName = firstName
        request.lastName = lastName
        
        TelegramClient.sendRequest(request)
    }
    
    override suspend fun logout() {
        Logger.d(TAG, "Logging out")
        TelegramClient.sendRequest(TdApi.LogOut())
    }
    
    /**
     * Map TDLib authorization state to domain model AuthState.
     */
    private fun mapAuthorizationState(state: TdApi.AuthorizationState): AuthState {
        return when (state) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                AuthState.WaitingForTDLibParameters
            }
            is TdApi.AuthorizationStateWaitEncryptionKey -> {
                AuthState.WaitingForEncryptionKey
            }
            is TdApi.AuthorizationStateWaitPhoneNumber -> {
                AuthState.WaitingForPhoneNumber
            }
            is TdApi.AuthorizationStateWaitCode -> {
                AuthState.WaitingForCode(state.codeInfo.type.toString())
            }
            is TdApi.AuthorizationStateWaitOtherDeviceConfirmation -> {
                AuthState.WaitingForDeviceConfirmation(state.link)
            }
            is TdApi.AuthorizationStateWaitPassword -> {
                AuthState.WaitingForPassword(state.passwordHint, state.hasRecoveryEmailAddress)
            }
            is TdApi.AuthorizationStateWaitRegistration -> {
                AuthState.WaitingForRegistration
            }
            is TdApi.AuthorizationStateReady -> {
                AuthState.Authenticated
            }
            is TdApi.AuthorizationStateLoggingOut -> {
                AuthState.LoggingOut
            }
            is TdApi.AuthorizationStateClosing -> {
                AuthState.Closing
            }
            is TdApi.AuthorizationStateClosed -> {
                AuthState.Closed
            }
            else -> {
                AuthState.Unknown
            }
        }
    }
}
```

---

## Part 5: Hilt Dependency Injection

Create a Hilt module to provide the TelegramRepository as a singleton.

**File: `data/src/main/java/com/swiftgram/data/di/RepositoryModule.kt`**

```kotlin
package com.swiftgram.data.di

import com.swiftgram.data.repository.TelegramRepositoryImpl
import com.swiftgram.domain.repository.TelegramRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing repository implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindTelegramRepository(
        impl: TelegramRepositoryImpl
    ): TelegramRepository
}
```

---

## Part 6: Update Domain Models

Update the `AuthState` model in the domain layer to represent all authentication states.

**File: `domain/src/main/java/com/swiftgram/domain/model/AuthState.kt`**

```kotlin
package com.swiftgram.domain.model

/**
 * Sealed class representing the current authentication state.
 * Used to drive the UI and determine which screen to display.
 */
sealed class AuthState {
    object WaitingForTDLibParameters : AuthState()
    object WaitingForEncryptionKey : AuthState()
    object WaitingForPhoneNumber : AuthState()
    data class WaitingForCode(val codeType: String) : AuthState()
    data class WaitingForDeviceConfirmation(val link: String) : AuthState()
    data class WaitingForPassword(val hint: String, val hasRecoveryEmail: Boolean) : AuthState()
    object WaitingForRegistration : AuthState()
    object Authenticated : AuthState()
    object LoggingOut : AuthState()
    object Closing : AuthState()
    object Closed : AuthState()
    data class Error(val message: String) : AuthState()
    object Unknown : AuthState()
}
```

---

## Part 7: Update Core Module Dependencies

Ensure the `core/build.gradle.kts` includes necessary dependencies.

**Update: `core/build.gradle.kts`**

```kotlin
dependencies {
    // ... existing dependencies ...
    
    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // ... rest of dependencies ...
}
```

---

## Part 8: Testing the TDLib Client

Create a simple test to verify the TDLib client initialization.

**File: `data/src/test/java/com/swiftgram/data/remote/telegram/TelegramClientTest.kt`**

```kotlin
package com.swiftgram.data.remote.telegram

import org.junit.Test
import org.junit.Before

class TelegramClientTest {
    
    @Before
    fun setUp() {
        // Reset client before each test
        TelegramClient.reset()
    }
    
    @Test
    fun testClientInitialization() {
        // This is a basic test; full testing requires Android context
        assert(!TelegramClient.isInitialized())
        assert(!TelegramClient.isConnected())
    }
}
```

---

## Part 9: Usage Example

Here's how to use the TelegramClient in your application:

```kotlin
// In your ViewModel or Activity
class AuthViewModel @Inject constructor(
    private val telegramRepository: TelegramRepository
) : ViewModel() {
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    fun initialize(apiId: Int, apiHash: String) {
        viewModelScope.launch {
            try {
                telegramRepository.initialize(apiId, apiHash)
                telegramRepository.connect()
                
                // Observe auth state changes
                telegramRepository.observeAuthorizationState().collect { state ->
                    _authState.value = state
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun sendPhoneNumber(phoneNumber: String) {
        viewModelScope.launch {
            try {
                telegramRepository.sendPhoneNumber(phoneNumber)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Failed to send phone number")
            }
        }
    }
}
```

---

## Troubleshooting

### Issue: `UnsatisfiedLinkError: libtdjson.so not found`

**Solution:**
- Ensure TDLib native libraries are properly placed in `app/src/main/jniLibs/{abi}/`
- Verify the device ABI matches the library architecture
- Rebuild the project: `./gradlew clean assembleDebug`

### Issue: `TelegramClient not initialized` exception

**Solution:**
- Call `TelegramClient.initialize()` before any other operations
- Ensure it's called from a coroutine context

### Issue: No updates received from TDLib

**Solution:**
- Verify `TelegramClient.connect()` was called successfully
- Check that TDLib parameters were set correctly
- Monitor logs for error messages

---

## Next Steps

Once Step 2 is complete:
- Verify TelegramClient initialization and connection
- Test that updates are received and emitted correctly
- Proceed to **Step 3: Domain Models & Use Cases** to create authentication use cases

