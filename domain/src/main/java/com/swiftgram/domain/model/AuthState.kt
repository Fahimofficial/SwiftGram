package com.swiftgram.domain.model

/**
 * Sealed class representing the current authentication state.
 * Used to drive the UI and determine which screen to display.
 *
 * The authentication flow follows this typical sequence:
 * 1. WaitingForTDLibParameters - Initial state while TDLib is initializing
 * 2. WaitingForEncryptionKey - Database encryption setup
 * 3. WaitingForPhoneNumber - Ready for user to enter phone number
 * 4. WaitingForCode - Waiting for SMS/Telegram code
 * 5. (Optional) WaitingForPassword - If 2FA is enabled
 * 6. (Optional) WaitingForRegistration - If account doesn't exist
 * 7. Authenticated - Successfully logged in
 */
sealed class AuthState {
    
    /**
     * TDLib is initializing and setting up parameters.
     * Show a loading screen during this state.
     */
    object WaitingForTDLibParameters : AuthState()
    
    /**
     * Setting up database encryption.
     * Show a loading screen during this state.
     */
    object WaitingForEncryptionKey : AuthState()
    
    /**
     * Ready for user to enter their phone number.
     * Show the phone number input screen.
     */
    object WaitingForPhoneNumber : AuthState()
    
    /**
     * Waiting for the user to enter the verification code.
     * Show the code input screen.
     *
     * @param codeType The type of code delivery (e.g., "SMS", "Telegram")
     */
    data class WaitingForCode(val codeType: String) : AuthState()
    
    /**
     * Waiting for confirmation from another device.
     * Show a screen with the link for the user to confirm on another device.
     *
     * @param link The link to confirm on another device
     */
    data class WaitingForDeviceConfirmation(val link: String) : AuthState()
    
    /**
     * Waiting for 2FA password.
     * Show the password input screen.
     *
     * @param hint Password hint provided by the user
     * @param hasRecoveryEmail Whether the account has a recovery email
     */
    data class WaitingForPassword(val hint: String, val hasRecoveryEmail: Boolean) : AuthState()
    
    /**
     * New user registration required.
     * Show the registration screen for new users.
     */
    object WaitingForRegistration : AuthState()
    
    /**
     * User is authenticated and can use the app.
     * Navigate to the main app screen.
     */
    object Authenticated : AuthState()
    
    /**
     * User is in the process of logging out.
     * Show a loading screen.
     */
    object LoggingOut : AuthState()
    
    /**
     * The client is closing.
     * Show a loading screen.
     */
    object Closing : AuthState()
    
    /**
     * The client is closed.
     * This is a terminal state; the app may need to restart.
     */
    object Closed : AuthState()
    
    /**
     * An error occurred during authentication.
     * Show an error message and allow the user to retry.
     *
     * @param message Human-readable error message
     */
    data class Error(val message: String) : AuthState()
    
    /**
     * Unknown state (shouldn't normally occur).
     * Show a loading screen or generic state.
     */
    object Unknown : AuthState()
}
