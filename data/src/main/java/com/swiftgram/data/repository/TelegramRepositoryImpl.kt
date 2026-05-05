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
 *
 * This class bridges the gap between the domain layer (which uses domain models)
 * and the data layer (which uses TDLib). It converts TDLib updates into domain models.
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
    
    /**
     * Observe authorization state changes.
     * Converts TDLib updates into domain AuthState objects.
     *
     * This Flow will emit:
     * - WaitingForPhoneNumber when ready for phone input
     * - WaitingForCode when waiting for verification code
     * - WaitingForPassword when 2FA is required
     * - Authenticated when successfully logged in
     * - Error if something goes wrong
     *
     * @return Flow of AuthState updates
     */
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
     * This conversion ensures the UI layer doesn't depend on TDLib types.
     *
     * @param state The TDLib authorization state
     * @return The corresponding domain AuthState
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
                val codeType = when (state.codeInfo.type) {
                    is TdApi.AuthenticationCodeTypeSms -> "SMS"
                    is TdApi.AuthenticationCodeTypeTelegramMessage -> "Telegram"
                    is TdApi.AuthenticationCodeTypeCall -> "Call"
                    is TdApi.AuthenticationCodeTypeFlashCall -> "Flash Call"
                    else -> "Unknown"
                }
                AuthState.WaitingForCode(codeType)
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
