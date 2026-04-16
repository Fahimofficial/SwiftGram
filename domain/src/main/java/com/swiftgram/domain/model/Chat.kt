package com.swiftgram.domain.model

data class Chat(
    val id: Long,
    val title: String,
    val lastMessage: Message?,
    val unreadCount: Int,
    val photoUrl: String?,
    val isPinned: Boolean = false,
    val type: ChatType = ChatType.PRIVATE
)

enum class ChatType {
    PRIVATE, GROUP, CHANNEL
}
