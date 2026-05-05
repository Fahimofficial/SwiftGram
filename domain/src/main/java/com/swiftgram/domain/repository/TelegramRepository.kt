package com.swiftgram.domain.repository

import com.swiftgram.domain.model.AuthState
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Telegram operations.
 * Defines the contract for authentication, messaging, and chat operations.
 *
 * This interface abstracts away the TDLib implementation details,
 * allowing the rest of the application to work with domain models instead.
 */
interface TelegramRepository {
    
    /**
     * Initialize the Telegram client with API credentials.
     * Must be called before any other operations.
     *
     * @param apiId Telegram API ID from https://my.telegram.org/apps
     * @param apiHash Telegram API Hash from https://my.telegram.org/apps
     * @throws RuntimeException if initialization fails
     */
    suspend fun initialize(apiId: Int, apiHash: String)
    
    /**
     * Connect to Telegram servers.
     * Must be called after initialization.
     *
     * @throws IllegalStateException if not initialized
     */
    suspend fun connect()
    
    /**
     * Disconnect from Telegram servers.
     */
    suspend fun disconnect()
    
    /**
     * Get the current authorization state as a Flow.
     * Emits updates whenever the auth state changes.
     *
     * Collectors will receive:
     * - WaitingForPhoneNumber when the client is ready for phone input
     * - WaitingForCode when waiting for SMS/Telegram code
     * - WaitingForPassword when 2FA is enabled
     * - Authenticated when the user is logged in
     * - Error if something goes wrong
     *
     * @return Flow of AuthState updates
     */
    fun observeAuthorizationState(): Flow<AuthState>
    
    /**
     * Send a phone number for authentication.
     * Should be called when auth state is WaitingForPhoneNumber.
     *
     * @param phoneNumber The user's phone number (e.g., "+1234567890")
     */
    suspend fun sendPhoneNumber(phoneNumber: String)
    
    /**
     * Send the authentication code received via SMS or Telegram.
     * Should be called when auth state is WaitingForCode.
     *
     * @param code The 5-digit code (e.g., "12345")
     */
    suspend fun sendAuthenticationCode(code: String)
    
    /**
     * Send the password for 2FA authentication.
     * Should be called when auth state is WaitingForPassword.
     *
     * @param password The user's 2FA password
     */
    suspend fun sendPassword(password: String)
    
    /**
     * Register a new account (for new users).
     * Should be called when auth state is WaitingForRegistration.
     *
     * @param firstName User's first name
     * @param lastName User's last name (optional)
     */
    suspend fun registerUser(firstName: String, lastName: String = "")
    
    /**
     * Log out from the current account.
     * After logout, the client will need to re-authenticate.
     */
    suspend fun logout()
}
