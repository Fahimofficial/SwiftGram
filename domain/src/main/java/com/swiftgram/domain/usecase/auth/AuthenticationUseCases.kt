package com.swiftgram.domain.usecase.auth

import com.swiftgram.domain.model.AuthState
import com.swiftgram.domain.repository.TelegramRepository
import com.swiftgram.domain.usecase.NoParamUseCase
import com.swiftgram.domain.usecase.UseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for initializing the Telegram client.
 * Must be called before any other authentication operations.
 *
 * This use case:
 * 1. Initializes the TDLib client with API credentials
 * 2. Connects to Telegram servers
 * 3. Prepares the client for authentication
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

/**
 * Use case for sending the user's phone number for authentication.
 * Should be called when the auth state is WaitingForPhoneNumber.
 *
 * Validates the phone number format before sending.
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
        
        if (params.phoneNumber.length < 10) {
            throw IllegalArgumentException("Phone number is too short")
        }
        
        telegramRepository.sendPhoneNumber(params.phoneNumber)
    }
}

/**
 * Use case for verifying the authentication code.
 * Should be called when the auth state is WaitingForCode.
 *
 * Validates the code format before sending.
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
            throw IllegalArgumentException("Code is too short (minimum 3 characters)")
        }
        
        if (params.code.length > 10) {
            throw IllegalArgumentException("Code is too long (maximum 10 characters)")
        }
        
        telegramRepository.sendAuthenticationCode(params.code)
    }
}

/**
 * Use case for verifying the 2FA password.
 * Should be called when the auth state is WaitingForPassword.
 *
 * Validates the password before sending.
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
        
        if (params.password.length < 1) {
            throw IllegalArgumentException("Password is too short")
        }
        
        telegramRepository.sendPassword(params.password)
    }
}

/**
 * Use case for registering a new user account.
 * Should be called when the auth state is WaitingForRegistration.
 *
 * Validates user information before sending.
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
        
        if (params.firstName.length > 64) {
            throw IllegalArgumentException("First name is too long (maximum 64 characters)")
        }
        
        if (params.lastName.length > 64) {
            throw IllegalArgumentException("Last name is too long (maximum 64 characters)")
        }
        
        telegramRepository.registerUser(params.firstName, params.lastName)
    }
}

/**
 * Use case for observing authentication state changes.
 * Returns a Flow that emits auth state updates whenever the authentication state changes.
 *
 * This use case doesn't use the standard UseCase base class because it returns a Flow
 * rather than a single result. The Flow will emit updates for the lifetime of the collector.
 *
 * Usage:
 * ```
 * observeAuthStateUseCase().collect { authState ->
 *     when (authState) {
 *         is AuthState.WaitingForPhoneNumber -> { /* show phone input */ }
 *         is AuthState.WaitingForCode -> { /* show code input */ }
 *         is AuthState.Authenticated -> { /* navigate to main */ }
 *         is AuthState.Error -> { /* show error */ }
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

/**
 * Use case for logging out the current user.
 * Sends a logout request to Telegram servers.
 *
 * After logout, the user will need to re-authenticate.
 */
class LogoutUseCase @Inject constructor(
    private val telegramRepository: TelegramRepository
) : NoParamUseCase<Unit>() {
    
    override suspend fun execute() {
        telegramRepository.logout()
    }
}
