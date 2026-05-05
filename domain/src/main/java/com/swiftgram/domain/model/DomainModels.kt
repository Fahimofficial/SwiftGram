package com.swiftgram.domain.model

/**
 * Domain model representing a Telegram user.
 *
 * @param id Unique identifier for the user
 * @param firstName User's first name
 * @param lastName User's last name (optional)
 * @param phoneNumber User's phone number (only visible to contacts)
 * @param profilePhotoUrl URL to the user's profile photo
 * @param isBot Whether this user is a bot
 * @param isVerified Whether this user is verified by Telegram
 * @param status Current online status of the user
 */
data class User(
    val id: Long,
    val firstName: String,
    val lastName: String = "",
    val phoneNumber: String = "",
    val profilePhotoUrl: String? = null,
    val isBot: Boolean = false,
    val isVerified: Boolean = false,
    val status: UserStatus = UserStatus.Offline
)

/**
 * Sealed class representing the online status of a user.
 */
sealed class UserStatus {
    object Online : UserStatus()
    object Offline : UserStatus()
    data class LastSeen(val timestamp: Long) : UserStatus()
}

/**
 * Domain model representing a Telegram chat.
 *
 * @param id Unique identifier for the chat
 * @param title Display name of the chat
 * @param type Type of chat (private, group, supergroup, channel)
 * @param unreadCount Number of unread messages
 * @param lastMessageDate Timestamp of the last message
 * @param photoUrl URL to the chat's profile photo
 * @param isArchived Whether the chat is archived
 * @param isPinned Whether the chat is pinned to the top
 * @param lastMessage The last message in the chat
 */
data class Chat(
    val id: Long,
    val title: String,
    val type: ChatType,
    val unreadCount: Int = 0,
    val lastMessageDate: Long = 0,
    val photoUrl: String? = null,
    val isArchived: Boolean = false,
    val isPinned: Boolean = false,
    val lastMessage: Message? = null
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
    data class Photo(val photoUrl: String, val caption: String = "") : MessageContent()
    data class Video(val videoUrl: String, val duration: Int, val caption: String = "") : MessageContent()
    data class Document(val fileName: String, val fileSize: Long, val mimeType: String = "") : MessageContent()
    data class Voice(val duration: Int) : MessageContent()
    data class Location(val latitude: Double, val longitude: Double) : MessageContent()
    data class Contact(val phoneNumber: String, val firstName: String, val lastName: String = "") : MessageContent()
}

/**
 * Domain model for authentication state.
 * Represents the different states during the authentication flow.
 */
sealed class AuthState {
    object Unknown : AuthState()
    object WaitingForTDLibParameters : AuthState()
    object WaitingForEncryptionKey : AuthState()
    object WaitingForPhoneNumber : AuthState()
    data class WaitingForCode(val codeType: String) : AuthState()
    data class WaitingForDeviceConfirmation(val link: String) : AuthState()
    data class WaitingForPassword(val hint: String = "", val hasRecoveryEmail: Boolean = false) : AuthState()
    object WaitingForRegistration : AuthState()
    object Authenticated : AuthState()
    object LoggingOut : AuthState()
    object Closing : AuthState()
    object Closed : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * Domain model for a chat list item (simplified version for display).
 *
 * @param id Chat ID
 * @param title Chat title
 * @param lastMessage Preview of the last message
 * @param unreadCount Number of unread messages
 * @param timestamp Timestamp of the last message
 * @param photoUrl URL to the chat's photo
 */
data class ChatListItem(
    val id: Long,
    val title: String,
    val lastMessage: String = "",
    val unreadCount: Int = 0,
    val timestamp: Long = 0,
    val photoUrl: String? = null
)

/**
 * Domain model for search results.
 *
 * @param messages List of messages matching the search query
 * @param chats List of chats matching the search query
 * @param users List of users matching the search query
 */
data class SearchResults(
    val messages: List<Message> = emptyList(),
    val chats: List<Chat> = emptyList(),
    val users: List<User> = emptyList()
)
