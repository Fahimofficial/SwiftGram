package com.swiftgram.domain.model

/**
 * Domain model representing a Telegram chat.
 * This is a simplified version that can be extended as needed.
 *
 * @param id Unique identifier for the chat
 * @param title Display name of the chat
 * @param type Type of chat (private, group, supergroup, channel)
 * @param unreadCount Number of unread messages
 * @param lastMessageDate Timestamp of the last message
 * @param photoUrl URL to the chat's profile photo
 * @param isArchived Whether the chat is archived
 * @param isPinned Whether the chat is pinned to the top
 */
data class Chat(
    val id: Long,
    val title: String,
    val type: ChatType,
    val unreadCount: Int = 0,
    val lastMessageDate: Long = 0,
    val photoUrl: String? = null,
    val isArchived: Boolean = false,
    val isPinned: Boolean = false
)

/**
 * Sealed class representing the type of a Telegram chat.
 */
sealed class ChatType {
    object Private : ChatType()
    object Group : ChatType()
    object Supergroup : ChatType()
    object Channel : ChatType()
}

/**
 * Domain model representing a Telegram message.
 * This is a simplified version that can be extended as needed.
 *
 * @param id Unique identifier for the message
 * @param chatId ID of the chat containing this message
 * @param senderId ID of the user who sent the message
 * @param text Text content of the message
 * @param date Timestamp when the message was sent
 * @param isOutgoing Whether this message was sent by the current user
 * @param isEdited Whether this message has been edited
 * @param replyToMessageId ID of the message this message replies to (if any)
 * @param content The content of the message (text, photo, video, etc.)
 */
data class Message(
    val id: Long,
    val chatId: Long,
    val senderId: Long,
    val text: String = "",
    val date: Long,
    val isOutgoing: Boolean = false,
    val isEdited: Boolean = false,
    val replyToMessageId: Long? = null,
    val content: MessageContent = MessageContent.Text
)

/**
 * Sealed class representing the content type of a message.
 */
sealed class MessageContent {
    object Text : MessageContent()
    data class Photo(val photoUrl: String) : MessageContent()
    data class Video(val videoUrl: String, val duration: Int) : MessageContent()
    data class Document(val fileName: String, val fileSize: Long) : MessageContent()
    data class Voice(val duration: Int) : MessageContent()
}

/**
 * Domain model representing a Telegram user.
 * This is a simplified version that can be extended as needed.
 *
 * @param id Unique identifier for the user
 * @param firstName User's first name
 * @param lastName User's last name
 * @param phoneNumber User's phone number (only visible to contacts)
 * @param profilePhotoUrl URL to the user's profile photo
 * @param status Current online status of the user
 * @param isBot Whether this user is a bot
 * @param isVerified Whether this user is verified by Telegram
 */
data class User(
    val id: Long,
    val firstName: String,
    val lastName: String = "",
    val phoneNumber: String = "",
    val profilePhotoUrl: String? = null,
    val status: UserStatus = UserStatus.Offline,
    val isBot: Boolean = false,
    val isVerified: Boolean = false
)

/**
 * Sealed class representing the online status of a user.
 */
sealed class UserStatus {
    object Online : UserStatus()
    object Offline : UserStatus()
    data class LastSeen(val timestamp: Long) : UserStatus()
}
