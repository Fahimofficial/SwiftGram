package com.swiftgram.domain.usecase

import com.swiftgram.domain.model.AuthState
import com.swiftgram.domain.model.Chat
import com.swiftgram.domain.model.ChatListItem
import com.swiftgram.domain.model.Message
import com.swiftgram.domain.repository.TelegramRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for initializing the Telegram client.
 * Must be called before any other authentication operations.
 */
class InitializeTelegramUseCase @Inject constructor(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(apiId: Int, apiHash: String) {
        repository.initialize(apiId, apiHash)
        repository.connect()
    }
}

/**
 * Use case for observing authentication state changes.
 * Returns a Flow that emits auth state updates.
 */
class ObserveAuthStateUseCase @Inject constructor(
    private val repository: TelegramRepository
) {
    operator fun invoke(): Flow<AuthState> {
        return repository.observeAuthorizationState()
    }
}

/**
 * Use case for sending the user's phone number.
 * Should be called when auth state is WaitingForPhoneNumber.
 */
class SendPhoneNumberUseCase @Inject constructor(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(phoneNumber: String) {
        if (phoneNumber.isEmpty()) {
            throw IllegalArgumentException("Phone number cannot be empty")
        }
        if (!phoneNumber.startsWith("+")) {
            throw IllegalArgumentException("Phone number must start with +")
        }
        repository.sendPhoneNumber(phoneNumber)
    }
}

/**
 * Use case for verifying the authentication code.
 * Should be called when auth state is WaitingForCode.
 */
class VerifyCodeUseCase @Inject constructor(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(code: String) {
        if (code.isEmpty()) {
            throw IllegalArgumentException("Code cannot be empty")
        }
        if (code.length < 3) {
            throw IllegalArgumentException("Code is too short")
        }
        repository.sendAuthenticationCode(code)
    }
}

/**
 * Use case for verifying the 2FA password.
 * Should be called when auth state is WaitingForPassword.
 */
class VerifyPasswordUseCase @Inject constructor(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(password: String) {
        if (password.isEmpty()) {
            throw IllegalArgumentException("Password cannot be empty")
        }
        repository.sendPassword(password)
    }
}

/**
 * Use case for registering a new user account.
 * Should be called when auth state is WaitingForRegistration.
 */
class RegisterUserUseCase @Inject constructor(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(firstName: String, lastName: String = "") {
        if (firstName.isEmpty()) {
            throw IllegalArgumentException("First name cannot be empty")
        }
        if (firstName.length < 2) {
            throw IllegalArgumentException("First name must be at least 2 characters")
        }
        repository.registerUser(firstName, lastName)
    }
}

/**
 * Use case for logging out the current user.
 */
class LogoutUseCase @Inject constructor(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke() {
        repository.logout()
    }
}

/**
 * Use case for getting all chats.
 * Returns a list of chat list items for display.
 */
class GetChatsUseCase @Inject constructor(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(): List<ChatListItem> {
        // TODO: Implement chat retrieval
        return emptyList()
    }
}

/**
 * Use case for getting messages from a specific chat.
 */
class GetMessagesUseCase @Inject constructor(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(chatId: Long, limit: Int = 50): List<Message> {
        // TODO: Implement message retrieval
        return emptyList()
    }
}

/**
 * Use case for sending a message to a chat.
 */
class SendMessageUseCase @Inject constructor(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(chatId: Long, text: String) {
        if (text.isEmpty()) {
            throw IllegalArgumentException("Message text cannot be empty")
        }
        if (text.length > 4096) {
            throw IllegalArgumentException("Message is too long (max 4096 characters)")
        }
        // TODO: Implement message sending
    }
}

/**
 * Use case for searching messages.
 */
class SearchMessagesUseCase @Inject constructor(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(query: String, limit: Int = 50): List<Message> {
        if (query.isEmpty()) {
            throw IllegalArgumentException("Search query cannot be empty")
        }
        // TODO: Implement message search
        return emptyList()
    }
}
