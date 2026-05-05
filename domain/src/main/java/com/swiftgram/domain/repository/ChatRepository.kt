package com.swiftgram.domain.repository

import com.swiftgram.domain.model.Chat
import com.swiftgram.domain.model.Message
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for chat and message operations.
 * Defines the contract for retrieving and managing chats and messages.
 *
 * This interface abstracts away the implementation details of how chats and messages
 * are fetched from TDLib or cached locally.
 */
interface ChatRepository {
    
    /**
     * Get all chats as a Flow.
     * Emits updates whenever the chat list changes (new chats, deleted chats, etc.).
     *
     * @return Flow of chat lists
     */
    fun observeChats(): Flow<List<Chat>>
    
    /**
     * Get a specific chat by ID.
     * Returns null if the chat is not found.
     *
     * @param chatId ID of the chat to retrieve
     * @return The chat, or null if not found
     */
    suspend fun getChat(chatId: Long): Chat?
    
    /**
     * Get messages from a chat.
     * Returns messages in reverse chronological order (newest first).
     *
     * @param chatId ID of the chat
     * @param limit Maximum number of messages to retrieve (default 50)
     * @param offset Number of messages to skip (for pagination)
     * @return List of messages
     */
    suspend fun getMessages(chatId: Long, limit: Int = 50, offset: Int = 0): List<Message>
    
    /**
     * Send a text message to a chat.
     *
     * @param chatId ID of the chat to send to
     * @param text Message text
     * @return The sent message
     */
    suspend fun sendMessage(chatId: Long, text: String): Message
    
    /**
     * Edit an existing message.
     *
     * @param chatId ID of the chat containing the message
     * @param messageId ID of the message to edit
     * @param text New message text
     * @return The edited message
     */
    suspend fun editMessage(chatId: Long, messageId: Long, text: String): Message
    
    /**
     * Delete a message.
     *
     * @param chatId ID of the chat containing the message
     * @param messageId ID of the message to delete
     */
    suspend fun deleteMessage(chatId: Long, messageId: Long)
    
    /**
     * Search messages in a chat.
     *
     * @param chatId ID of the chat to search in
     * @param query Search query
     * @param limit Maximum number of results (default 50)
     * @return List of matching messages
     */
    suspend fun searchMessages(chatId: Long, query: String, limit: Int = 50): List<Message>
    
    /**
     * Mark a chat as read (all messages before the specified message ID).
     *
     * @param chatId ID of the chat
     * @param messageId ID of the message up to which to mark as read
     */
    suspend fun markChatAsRead(chatId: Long, messageId: Long)
    
    /**
     * Pin a message in a chat.
     *
     * @param chatId ID of the chat
     * @param messageId ID of the message to pin
     */
    suspend fun pinMessage(chatId: Long, messageId: Long)
    
    /**
     * Unpin a message in a chat.
     *
     * @param chatId ID of the chat
     * @param messageId ID of the message to unpin
     */
    suspend fun unpinMessage(chatId: Long, messageId: Long)
}
