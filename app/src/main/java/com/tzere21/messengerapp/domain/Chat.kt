package com.tzere21.messengerapp.domain

data class Message(
    val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)

data class UserChat(
    val chatId: String = ""
)