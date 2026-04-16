package com.swiftgram.domain.model

data class Message(
    val id: Long,
    val chatId: Long,
    val senderId: Long,
    val text: String,
    val timestamp: Long,
    val isFromMe: Boolean,
    val status: MessageStatus = MessageStatus.SENT,
    val media: Media? = null
)

enum class MessageStatus {
    SENDING, SENT, DELIVERED, READ, FAILED
}

sealed class Media {
    data class Photo(val url: String, val width: Int, val height: Int) : Media()
    data class Video(val url: String, val duration: Int) : Media()
    data class Voice(val url: String, val duration: Int) : Media()
    data class Document(val url: String, val fileName: String, val fileSize: Long) : Media()
}
