# Step 3: Domain Models & Use Cases

This document provides complete implementation details for creating domain models and use cases that orchestrate the authentication flow.

## Overview

**Objective:** Build a clean domain layer that:
1. Defines domain models for authentication operations
2. Creates use cases (interactors) that encapsulate business logic
3. Provides a clear contract between the UI and data layers
4. Maintains independence from framework-specific code

**Architecture:**
- **Domain Models** – Pure Kotlin data classes representing authentication states and operations
- **Use Cases** – Single-responsibility interactors for specific operations
- **Result Wrapper** – Generic result type for handling success/error states
- **Repository Interface** – Already defined in Step 2

---

## Part 1: Result Wrapper

Create a generic Result wrapper for handling operation outcomes.

**File: `domain/src/main/java/com/swiftgram/domain/common/Result.kt`**

```kotlin
package com.swiftgram.domain.common

/**
 * Sealed class representing the result of an operation.
 * Can be either Success with data, or Failure with an exception.
 *
 * Usage:
 * ```
 * when (result) {
 *     is Result.Success -> { /* handle data */ }
 *     is Result.Failure -> { /* handle error */ }
 * }
 * ```
 */
sealed class Result<out T> {
    
    /**
     * Operation succeeded with data.
     */
    data class Success<T>(val data: T) : Result<T>()
    
    /**
     * Operation failed with an exception.
     */
    data class Failure(val exception: Exception) : Result<Nothing>()
    
    /**
     * Get the data if successful, or null if failed.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }
    
    /**
     * Get the exception if failed, or null if successful.
     */
    fun exceptionOrNull(): Exception? = when (this) {
        is Success -> null
        is Failure -> exception
    }
    
    /**
     * Transform the success data using the provided function.
     */
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> this
    }
    
    /**
     * Apply a side effect for both success and failure.
     */
    inline fun onEach(action: (T) -> Unit): Result<T> {
        if (this is Success) {
            action(data)
        }
        return this
    }
}
```

---

## Part 2: Use Case Base Class

Create a base class for all use cases to follow a consistent pattern.

**File: `domain/src/main/java/com/swiftgram/domain/usecase/UseCase.kt`**

```kotlin
package com.swiftgram.domain.usecase

import com.swiftgram.domain.common.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Base class for all use cases (interactors).
 * Provides a consistent pattern for executing domain logic.
 *
 * Usage:
 * ```
 * class MyUseCase @Inject constructor(
 *     private val repository: MyRepository
 * ) : UseCase<InputParams, OutputData>() {
 *     override suspend fun execute(params: InputParams): OutputData {
 *         return repository.doSomething(params)
 *     }
 * }
 * ```
 */
abstract class UseCase<in Input, out Output> {
    
    /**
     * Execute the use case with the given input.
     * Implement this in subclasses with the actual business logic.
     *
     * @param params Input parameters for the use case
     * @return The output of the use case
     */
    protected abstract suspend fun execute(params: Input): Output
    
    /**
     * Invoke the use case, handling coroutine dispatching and error wrapping.
     *
     * @param params Input parameters
     * @return Result containing the output or exception
     */
    suspend operator fun invoke(params: Input): Result<Output> = withContext(Dispatchers.Default) {
        try {
            Result.Success(execute(params))
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}

/**
 * Base class for use cases that don't require input parameters.
 */
abstract class NoParamUseCase<out Output> {
    
    protected abstract suspend fun execute(): Output
    
    suspend operator fun invoke(): Result<Output> = withContext(Dispatchers.Default) {
        try {
            Result.Success(execute())
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}
```

---

## Part 3: Authentication Use Cases

Create use cases for each step of the authentication flow.

**File: `domain/src/main/java/com/swiftgram/domain/usecase/auth/InitializeTelegramUseCase.kt`**

```kotlin
package com.swiftgram.domain.usecase.auth

import com.swiftgram.domain.repository.TelegramRepository
import com.swiftgram.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Use case for initializing the Telegram client.
 * Must be called before any other authentication operations.
 */
class InitializeTelegramUseCase @Inject constructor(
    private val telegramRepository: TelegramRepository
) : UseCase<InitializeTelegramUseCase.Params, Unit>() {
    
    data class Params(
        val apiId: Int,
        val apiHash: String
    )
    
    override suspend fun execute(params: Params) {
        telegramRepository.initialize(params.apiId, params.apiHash)
        telegramRepository.connect()
    }
}
```

**File: `domain/src/main/java/com/swiftgram/domain/usecase/auth/SendPhoneNumberUseCase.kt`**

```kotlin
package com.swiftgram.domain.usecase.auth

import com.swiftgram.domain.repository.TelegramRepository
import com.swiftgram.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Use case for sending the user's phone number for authentication.
 * Should be called when the auth state is WaitingForPhoneNumber.
 */
class SendPhoneNumberUseCase @Inject constructor(
    private val telegramRepository: TelegramRepository
) : UseCase<SendPhoneNumberUseCase.Params, Unit>() {
    
    data class Params(
        val phoneNumber: String
    )
    
    override suspend fun execute(params: Params) {
        // Validate phone number format
        if (params.phoneNumber.isEmpty()) {
            throw IllegalArgumentException("Phone number cannot be empty")
        }
        
        if (!params.phoneNumber.startsWith("+")) {
            throw IllegalArgumentException("Phone number must start with +")
        }
        
        telegramRepository.sendPhoneNumber(params.phoneNumber)
    }
}
```

**File: `domain/src/main/java/com/swiftgram/domain/usecase/auth/VerifyAuthenticationCodeUseCase.kt`**

```kotlin
package com.swiftgram.domain.usecase.auth

import com.swiftgram.domain.repository.TelegramRepository
import com.swiftgram.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Use case for verifying the authentication code.
 * Should be called when the auth state is WaitingForCode.
 */
class VerifyAuthenticationCodeUseCase @Inject constructor(
    private val telegramRepository: TelegramRepository
) : UseCase<VerifyAuthenticationCodeUseCase.Params, Unit>() {
    
    data class Params(
        val code: String
    )
    
    override suspend fun execute(params: Params) {
        // Validate code format (typically 5 digits)
        if (params.code.isEmpty()) {
            throw IllegalArgumentException("Code cannot be empty")
        }
        
        if (params.code.length < 3) {
            throw IllegalArgumentException("Code is too short")
        }
        
        telegramRepository.sendAuthenticationCode(params.code)
    }
}
```

**File: `domain/src/main/java/com/swiftgram/domain/usecase/auth/VerifyPasswordUseCase.kt`**

```kotlin
package com.swiftgram.domain.usecase.auth

import com.swiftgram.domain.repository.TelegramRepository
import com.swiftgram.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Use case for verifying the 2FA password.
 * Should be called when the auth state is WaitingForPassword.
 */
class VerifyPasswordUseCase @Inject constructor(
    private val telegramRepository: TelegramRepository
) : UseCase<VerifyPasswordUseCase.Params, Unit>() {
    
    data class Params(
        val password: String
    )
    
    override suspend fun execute(params: Params) {
        if (params.password.isEmpty()) {
            throw IllegalArgumentException("Password cannot be empty")
        }
        
        telegramRepository.sendPassword(params.password)
    }
}
```

**File: `domain/src/main/java/com/swiftgram/domain/usecase/auth/RegisterUserUseCase.kt`**

```kotlin
package com.swiftgram.domain.usecase.auth

import com.swiftgram.domain.repository.TelegramRepository
import com.swiftgram.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Use case for registering a new user account.
 * Should be called when the auth state is WaitingForRegistration.
 */
class RegisterUserUseCase @Inject constructor(
    private val telegramRepository: TelegramRepository
) : UseCase<RegisterUserUseCase.Params, Unit>() {
    
    data class Params(
        val firstName: String,
        val lastName: String = ""
    )
    
    override suspend fun execute(params: Params) {
        if (params.firstName.isEmpty()) {
            throw IllegalArgumentException("First name cannot be empty")
        }
        
        if (params.firstName.length < 2) {
            throw IllegalArgumentException("First name must be at least 2 characters")
        }
        
        telegramRepository.registerUser(params.firstName, params.lastName)
    }
}
```

**File: `domain/src/main/java/com/swiftgram/domain/usecase/auth/ObserveAuthStateUseCase.kt`**

```kotlin
package com.swiftgram.domain.usecase.auth

import com.swiftgram.domain.model.AuthState
import com.swiftgram.domain.repository.TelegramRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for observing authentication state changes.
 * Returns a Flow that emits auth state updates.
 *
 * Usage:
 * ```
 * observeAuthStateUseCase().collect { authState ->
 *     when (authState) {
 *         is AuthState.WaitingForPhoneNumber -> { /* show phone input */ }
 *         is AuthState.WaitingForCode -> { /* show code input */ }
 *         is AuthState.Authenticated -> { /* navigate to main */ }
 *         else -> {}
 *     }
 * }
 * ```
 */
class ObserveAuthStateUseCase @Inject constructor(
    private val telegramRepository: TelegramRepository
) {
    
    operator fun invoke(): Flow<AuthState> {
        return telegramRepository.observeAuthorizationState()
    }
}
```

**File: `domain/src/main/java/com/swiftgram/domain/usecase/auth/LogoutUseCase.kt`**

```kotlin
package com.swiftgram.domain.usecase.auth

import com.swiftgram.domain.repository.TelegramRepository
import com.swiftgram.domain.usecase.NoParamUseCase
import javax.inject.Inject

/**
 * Use case for logging out the current user.
 */
class LogoutUseCase @Inject constructor(
    private val telegramRepository: TelegramRepository
) : NoParamUseCase<Unit>() {
    
    override suspend fun execute() {
        telegramRepository.logout()
    }
}
```

---

## Part 4: Hilt Use Case Module

Create a Hilt module to provide all use cases.

**File: `domain/src/main/java/com/swiftgram/domain/di/UseCaseModule.kt`**

```kotlin
package com.swiftgram.domain.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for providing use cases.
 * Use cases are provided as singletons since they're stateless.
 *
 * Note: Individual use cases are automatically provided by Hilt
 * when they have @Inject constructors. This module is a placeholder
 * for any future use case configuration.
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    // Use cases are automatically provided via their @Inject constructors
    // No explicit bindings needed
}
```

---

## Part 5: Domain Models for Chat and Messages

Create additional domain models for chat and message operations (for future use).

**File: `domain/src/main/java/com/swiftgram/domain/model/Chat.kt`**

```kotlin
package com.swiftgram.domain.model

/**
 * Domain model representing a Telegram chat.
 * This is a simplified version; extend as needed.
 */
data class Chat(
    val id: Long,
    val title: String,
    val type: ChatType,
    val unreadCount: Int = 0,
    val lastMessageDate: Long = 0,
    val photoUrl: String? = null,
    val isArchived: Boolean = false,
    val isPinned: Boolean = false
)

sealed class ChatType {
    object Private : ChatType()
    object Group : ChatType()
    object Supergroup : ChatType()
    object Channel : ChatType()
}
```

**File: `domain/src/main/java/com/swiftgram/domain/model/Message.kt`**

```kotlin
package com.swiftgram.domain.model

/**
 * Domain model representing a Telegram message.
 * This is a simplified version; extend as needed.
 */
data class Message(
    val id: Long,
    val chatId: Long,
    val senderId: Long,
    val text: String = "",
    val date: Long,
    val isOutgoing: Boolean = false,
    val isEdited: Boolean = false,
    val replyToMessageId: Long? = null,
    val content: MessageContent = MessageContent.Text
)

sealed class MessageContent {
    object Text : MessageContent()
    data class Photo(val photoUrl: String) : MessageContent()
    data class Video(val videoUrl: String, val duration: Int) : MessageContent()
    data class Document(val fileName: String, val fileSize: Long) : MessageContent()
    data class Voice(val duration: Int) : MessageContent()
}
```

**File: `domain/src/main/java/com/swiftgram/domain/model/User.kt`**

```kotlin
package com.swiftgram.domain.model

/**
 * Domain model representing a Telegram user.
 * This is a simplified version; extend as needed.
 */
data class User(
    val id: Long,
    val firstName: String,
    val lastName: String = "",
    val phoneNumber: String = "",
    val profilePhotoUrl: String? = null,
    val status: UserStatus = UserStatus.Offline,
    val isBot: Boolean = false,
    val isVerified: Boolean = false
)

sealed class UserStatus {
    object Online : UserStatus()
    object Offline : UserStatus()
    data class LastSeen(val timestamp: Long) : UserStatus()
}
```

---

## Part 6: Chat Repository Interface

Create a repository interface for chat operations (for future implementation).

**File: `domain/src/main/java/com/swiftgram/domain/repository/ChatRepository.kt`**

```kotlin
package com.swiftgram.domain.repository

import com.swiftgram.domain.model.Chat
import com.swiftgram.domain.model.Message
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for chat and message operations.
 * Defines the contract for retrieving and managing chats and messages.
 */
interface ChatRepository {
    
    /**
     * Get all chats as a Flow.
     * Emits updates whenever the chat list changes.
     */
    fun observeChats(): Flow<List<Chat>>
    
    /**
     * Get a specific chat by ID.
     */
    suspend fun getChat(chatId: Long): Chat?
    
    /**
     * Get messages from a chat.
     */
    suspend fun getMessages(chatId: Long, limit: Int = 50): List<Message>
    
    /**
     * Send a text message to a chat.
     */
    suspend fun sendMessage(chatId: Long, text: String): Message
    
    /**
     * Edit a message.
     */
    suspend fun editMessage(chatId: Long, messageId: Long, text: String): Message
    
    /**
     * Delete a message.
     */
    suspend fun deleteMessage(chatId: Long, messageId: Long)
    
    /**
     * Search messages in a chat.
     */
    suspend fun searchMessages(chatId: Long, query: String): List<Message>
}
```

---

## Part 7: Usage Example in ViewModel

Here's how to use the use cases in a ViewModel:

```kotlin
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
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        // Start observing auth state changes
        viewModelScope.launch {
            observeAuthStateUseCase().collect { state ->
                _authState.value = state
            }
        }
    }
    
    fun initialize(apiId: Int, apiHash: String) {
        viewModelScope.launch {
            val result = initializeTelegramUseCase(
                InitializeTelegramUseCase.Params(apiId, apiHash)
            )
            
            when (result) {
                is Result.Success -> {
                    // Initialization successful, auth state updates will flow
                }
                is Result.Failure -> {
                    _errorMessage.value = result.exception.message
                }
            }
        }
    }
    
    fun sendPhoneNumber(phoneNumber: String) {
        viewModelScope.launch {
            val result = sendPhoneNumberUseCase(
                SendPhoneNumberUseCase.Params(phoneNumber)
            )
            
            when (result) {
                is Result.Success -> {
                    // Phone number sent, waiting for code
                }
                is Result.Failure -> {
                    _errorMessage.value = result.exception.message
                }
            }
        }
    }
    
    fun verifyCode(code: String) {
        viewModelScope.launch {
            val result = verifyAuthenticationCodeUseCase(
                VerifyAuthenticationCodeUseCase.Params(code)
            )
            
            when (result) {
                is Result.Success -> {
                    // Code verified
                }
                is Result.Failure -> {
                    _errorMessage.value = result.exception.message
                }
            }
        }
    }
    
    fun verifyPassword(password: String) {
        viewModelScope.launch {
            val result = verifyPasswordUseCase(
                VerifyPasswordUseCase.Params(password)
            )
            
            when (result) {
                is Result.Success -> {
                    // Password verified
                }
                is Result.Failure -> {
                    _errorMessage.value = result.exception.message
                }
            }
        }
    }
    
    fun registerUser(firstName: String, lastName: String = "") {
        viewModelScope.launch {
            val result = registerUserUseCase(
                RegisterUserUseCase.Params(firstName, lastName)
            )
            
            when (result) {
                is Result.Success -> {
                    // User registered
                }
                is Result.Failure -> {
                    _errorMessage.value = result.exception.message
                }
            }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            val result = logoutUseCase()
            
            when (result) {
                is Result.Success -> {
                    // Logged out
                }
                is Result.Failure -> {
                    _errorMessage.value = result.exception.message
                }
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}
```

---

## Part 8: Testing Use Cases

Create unit tests for use cases.

**File: `domain/src/test/java/com/swiftgram/domain/usecase/auth/SendPhoneNumberUseCaseTest.kt`**

```kotlin
package com.swiftgram.domain.usecase.auth

import com.swiftgram.domain.common.Result
import com.swiftgram.domain.repository.TelegramRepository
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify

class SendPhoneNumberUseCaseTest {
    
    @Mock
    private lateinit var telegramRepository: TelegramRepository
    
    private lateinit var useCase: SendPhoneNumberUseCase
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        useCase = SendPhoneNumberUseCase(telegramRepository)
    }
    
    @Test
    fun testSendValidPhoneNumber() = runBlocking {
        val phoneNumber = "+1234567890"
        val params = SendPhoneNumberUseCase.Params(phoneNumber)
        
        val result = useCase(params)
        
        assert(result is Result.Success)
        verify(telegramRepository).sendPhoneNumber(phoneNumber)
    }
    
    @Test
    fun testSendInvalidPhoneNumber_Empty() = runBlocking {
        val params = SendPhoneNumberUseCase.Params("")
        
        val result = useCase(params)
        
        assert(result is Result.Failure)
    }
    
    @Test
    fun testSendInvalidPhoneNumber_NoPlus() = runBlocking {
        val params = SendPhoneNumberUseCase.Params("1234567890")
        
        val result = useCase(params)
        
        assert(result is Result.Failure)
    }
}
```

---

## Part 9: Folder Structure Summary

After completing Step 3, your project structure should look like:

```
domain/
├── src/main/java/com/swiftgram/domain/
│   ├── common/
│   │   └── Result.kt                    # Generic result wrapper
│   ├── di/
│   │   └── UseCaseModule.kt             # Hilt module
│   ├── model/
│   │   ├── AuthState.kt                 # Auth state (from Step 2)
│   │   ├── Chat.kt                      # Chat model
│   │   ├── Message.kt                   # Message model
│   │   └── User.kt                      # User model
│   ├── repository/
│   │   ├── TelegramRepository.kt         # Telegram repo (from Step 2)
│   │   └── ChatRepository.kt             # Chat repo
│   └── usecase/
│       ├── UseCase.kt                   # Base use case
│       └── auth/
│           ├── InitializeTelegramUseCase.kt
│           ├── SendPhoneNumberUseCase.kt
│           ├── VerifyAuthenticationCodeUseCase.kt
│           ├── VerifyPasswordUseCase.kt
│           ├── RegisterUserUseCase.kt
│           ├── ObserveAuthStateUseCase.kt
│           └── LogoutUseCase.kt
└── src/test/java/com/swiftgram/domain/
    └── usecase/auth/
        └── SendPhoneNumberUseCaseTest.kt
```

---

## Key Design Principles

**Single Responsibility:** Each use case handles one specific operation.

**Dependency Injection:** All dependencies are injected via Hilt.

**Error Handling:** Result wrapper provides type-safe error handling.

**Testability:** Use cases are easy to test with mock repositories.

**Flow-Based:** Async operations use Kotlin Coroutines and Flow.

**Domain Independence:** Domain layer has no dependencies on Android framework or TDLib.

---

## Next Steps

Once Step 3 is complete:
- Verify use cases compile without errors
- Write unit tests for use cases
- Proceed to **Step 4: Jetpack Compose UI** to build the authentication screens

