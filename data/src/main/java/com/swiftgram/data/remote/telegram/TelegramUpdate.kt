package com.swiftgram.data.remote.telegram

import org.drinkless.tdlib.TdApi

/**
 * Sealed class representing all possible TDLib updates.
 * Provides type-safe handling of Telegram events using Kotlin sealed classes.
 *
 * This allows the rest of the application to react to specific Telegram events
 * without dealing with raw TdApi.Object instances.
 */
sealed class TelegramUpdate {
    
    // ==================== Authentication Updates ====================
    
    /**
     * Authorization state has changed (e.g., waiting for phone, code, password, or authenticated).
     */
    data class AuthorizationStateUpdate(val state: TdApi.AuthorizationState) : TelegramUpdate()
    
    // ==================== Connection Updates ====================
    
    /**
     * Connection state has changed (connecting, connected, updating, or waiting).
     */
    data class ConnectionStateUpdate(val state: TdApi.ConnectionState) : TelegramUpdate()
    
    // ==================== Chat Updates ====================
    
    /**
     * A chat has been updated (title, photo, permissions, etc.).
     */
    data class ChatUpdate(val chat: TdApi.Chat) : TelegramUpdate()
    
    /**
     * The chat list has been updated (new chats added or removed).
     */
    data class ChatListUpdate(val chatList: TdApi.ChatList, val chatIds: LongArray) : TelegramUpdate()
    
    // ==================== Message Updates ====================
    
    /**
     * A new message has been received.
     */
    data class NewMessageUpdate(val message: TdApi.Message) : TelegramUpdate()
    
    /**
     * An existing message has been updated (edited, reactions added, etc.).
     */
    data class MessageUpdate(val message: TdApi.Message) : TelegramUpdate()
    
    /**
     * Messages have been deleted.
     */
    data class MessageDeletedUpdate(val chatId: Long, val messageIds: LongArray) : TelegramUpdate()
    
    // ==================== User Updates ====================
    
    /**
     * User information has been updated (name, status, etc.).
     */
    data class UserUpdate(val user: TdApi.User) : TelegramUpdate()
    
    /**
     * User's online status has changed.
     */
    data class UserStatusUpdate(val userId: Long, val status: TdApi.UserStatus) : TelegramUpdate()
    
    // ==================== Generic & Error Updates ====================
    
    /**
     * Generic update that doesn't fit into specific categories.
     * Use this for unhandled update types.
     */
    data class GenericUpdate(val update: TdApi.Object) : TelegramUpdate()
    
    /**
     * An error occurred during a TDLib operation.
     */
    data class ErrorUpdate(val error: TdApi.Error) : TelegramUpdate()
}
