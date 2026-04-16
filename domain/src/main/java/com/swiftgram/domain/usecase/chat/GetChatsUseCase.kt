package com.swiftgram.domain.usecase.chat

import com.swiftgram.domain.model.Chat
import com.swiftgram.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChatsUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(): Flow<List<Chat>> {
        return chatRepository.getChats()
    }
}
