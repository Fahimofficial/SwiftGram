package com.swiftgram.data.repository

import com.swiftgram.data.local.dao.ChatDao
import com.swiftgram.data.mapper.ChatMapper
import com.swiftgram.data.remote.api.TelegramApiService
import com.swiftgram.domain.model.Chat
import com.swiftgram.domain.model.Message
import com.swiftgram.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val telegramApiService: TelegramApiService,
    private val chatDao: ChatDao,
    private val chatMapper: ChatMapper
) : ChatRepository {

    override fun getChats(): Flow<List<Chat>> {
        // Fetch from local cache, assuming synchronization is handled elsewhere
        return chatDao.getAllChats().map { entities ->
            entities.map { chatMapper.mapToDomain(it) }
        }
    }

    override fun getMessages(chatId: Long): Flow<List<Message>> {
        // Implementation for getting messages for a specific chat
        return chatDao.getMessagesForChat(chatId).map { entities ->
            entities.map { chatMapper.mapToDomain(it) }
        }
    }

    override suspend fun getChatById(chatId: Long): Chat? {
        val chatEntity = chatDao.getChatById(chatId)
        return chatEntity?.let { chatMapper.mapToDomain(it) }
    }

    override suspend fun sendMessage(chatId: Long, text: String) {
        telegramApiService.sendMessage(chatId, text)
        // Optionally update local cache with the new message
    }

    override suspend fun deleteChat(chatId: Long) {
        telegramApiService.deleteChat(chatId)
        chatDao.deleteChat(chatId)
    }

    override suspend fun pinChat(chatId: Long, isPinned: Boolean) {
        telegramApiService.pinChat(chatId, isPinned)
        chatDao.updatePinStatus(chatId, isPinned)
    }
}
