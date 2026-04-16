package com.swiftgram.domain.repository

import com.swiftgram.domain.model.Chat
import com.swiftgram.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getChats(): Flow<List<Chat>>
    fun getMessages(chatId: Long): Flow<List<Message>>
    suspend fun getChatById(chatId: Long): Chat?
    suspend fun sendMessage(chatId: Long, text: String)
    suspend fun deleteChat(chatId: Long)
    suspend fun pinChat(chatId: Long, isPinned: Boolean)
}
