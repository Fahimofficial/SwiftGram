package com.swiftgram.data.remote.telegram

import android.content.Context
import android.os.Build
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
 * This class provides a clean Kotlin API for interacting with TDLib,
 * handling all the complexity of native library loading, request/response pairing,
 * and async event processing.
 *
 * Usage:
 * ```
 * val client = TelegramClient
 * client.initialize(context, apiId, apiHash)
 * client.connect()
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
    
    /**
     * Shared Flow for emitting TDLib updates to all collectors.
     * Uses replay=0 to not cache updates, and extraBufferCapacity=100 for handling bursts.
     */
    private val _updates = MutableSharedFlow<TelegramUpdate>(
        replay = 0,
        extraBufferCapacity = 100
    )
    val updates: SharedFlow<TelegramUpdate> = _updates.asSharedFlow()
    
    /**
     * Map of request IDs to their callbacks.
     * Used to pair TDLib responses with their corresponding requests.
     */
    private val requestCallbacks = ConcurrentHashMap<Long, (TdApi.Object) -> Unit>()
    private var nextRequestId = 1L
    
    /**
     * Initialize TDLib with the provided configuration.
     * Must be called once before any other operations.
     *
     * This function:
     * 1. Loads native TDLib libraries
     * 2. Creates the TDLib Client
     * 3. Configures TDLib parameters (database, files, API credentials, etc.)
     * 4. Sets up database encryption
     *
     * @param context Android context for accessing app directories
     * @param apiId Telegram API ID from https://my.telegram.org/apps
     * @param apiHash Telegram API Hash from https://my.telegram.org/apps
     * @throws RuntimeException if initialization fails
     */
    suspend fun initialize(
        context: Context,
        apiId: Int,
        apiHash: String
    ) = withContext(Dispatchers.Default) {
        if (isInitialized) {
            Logger.d(TAG, "TelegramClient already initialized")
            return@withContext
        }
        
        try {
            // Step 1: Load native TDLib libraries
            TDLibLoader.loadLibraries(context)
            Logger.d(TAG, "TDLib native libraries loaded")
            
            // Step 2: Set up logging (0 = errors only, 1 = warnings, 2 = info, 3 = debug)
            Client.setLogVerbosityLevel(0)
            
            // Step 3: Create the TDLib client with three callback functions
            // - First callback: for updates
            // - Second callback: for errors
            // - Third callback: for other events
            client = Client.create(
                { update -> handleTdLibUpdate(update) },
                { update -> handleTdLibUpdate(update) },
                { update -> handleTdLibUpdate(update) }
            )
            Logger.d(TAG, "TDLib Client created")
            
            // Step 4: Configure TDLib parameters
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
            setTdlibParameters.deviceModel = Build.MODEL
            setTdlibParameters.systemVersion = Build.VERSION.RELEASE
            setTdlibParameters.applicationVersion = "1.0.0"
            setTdlibParameters.enableStorageOptimizer = true
            setTdlibParameters.useSecondaryServerForSecretChats = true
            
            sendRequest(setTdlibParameters)
            Logger.d(TAG, "TDLib parameters configured")
            
            // Step 5: Set database encryption (empty key for no encryption)
            // In production, use a proper encryption key derived from user password
            val encryptionKey = TdApi.SetDatabaseEncryptionKey()
            encryptionKey.newEncryptionKey = ByteArray(0)
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
     *
     * This sends a request to get the current authorization state,
     * which triggers the auth state update flow.
     *
     * @throws IllegalStateException if not initialized
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
     * Sends a Close request to TDLib.
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
     * Send a request to TDLib.
     *
     * This is the primary method for sending requests to TDLib.
     * The response will be delivered via the callback or through the updates Flow.
     *
     * @param function The TDLib request function
     * @param callback Optional callback to receive the response
     * @return The request ID (useful for tracking)
     * @throws IllegalStateException if not initialized
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
            try {
                callback?.invoke(response)
            } finally {
                requestCallbacks.remove(requestId)
            }
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
     * @throws TimeoutException if the request times out
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
     *
     * This is called by TDLib whenever an update is received.
     * We convert the raw TdApi.Object into a typed TelegramUpdate and emit it.
     *
     * @param update The raw TDLib update object
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
            // tryEmit returns false if there are no collectors, which is fine
            _updates.tryEmit(telegramUpdate)
            
        } catch (e: Exception) {
            Logger.e(TAG, "Error handling TDLib update", e)
        }
    }
    
    /**
     * Get the current authorization state.
     *
     * @return The current AuthorizationState, or null if not available
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
     * This should be called before re-initializing the client.
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
