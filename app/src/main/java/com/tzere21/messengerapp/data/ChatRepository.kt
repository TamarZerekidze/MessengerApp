package com.tzere21.messengerapp.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import com.tzere21.messengerapp.domain.Message
import com.tzere21.messengerapp.domain.UserChat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ChatRepository {

    private val auth = Firebase.auth
    private val database = Firebase.database
    private val chatsRef = database.getReference("chats")
    private val userChatsRef = database.getReference("userChats")

    suspend fun getChatId(otherUserId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    return@withContext Result.failure(Exception("Not logged in"))
                }
                val currentUserId = currentUser.uid

                val chatsSnapshot = chatsRef.get().await()

                var chatId = ""
                for (chatSnapshot in chatsSnapshot.children) {
                    val participants = chatSnapshot.child("participants").getValue<List<String>>()

                    if (participants != null &&
                        participants.contains(currentUserId) &&
                        participants.contains(otherUserId)) {

                        val key = chatSnapshot.key
                        if (key != null) {
                            chatId = key
                            break
                        }
                    }
                }

                if (chatId.isEmpty()) {
                    chatId = createNewChat(currentUserId, otherUserId)
                }
                Result.success(chatId)

            } catch (e: Exception) {
                Log.e("MessageRepository", "Error getting/creating chat", e)
                Result.failure(e)
            }
        }
    }

    private suspend fun createNewChat(currentUserId: String, otherUserId: String): String {
        val chatId = chatsRef.push().key ?: throw Exception("Failed to generate chat ID")

        val chatData = mapOf(
            "chatId" to chatId,
            "participants" to listOf(currentUserId, otherUserId),
            "lastMessage" to null
        )

        val userChat = UserChat(
            chatId = chatId
        )

        chatsRef.child(chatId).setValue(chatData).await()
        userChatsRef.child(currentUserId).child(chatId).setValue(userChat).await()
        userChatsRef.child(otherUserId).child(chatId).setValue(userChat).await()

        return chatId
    }

    suspend fun sendMessage(chatId: String, receiverId: String, text: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    return@withContext Result.failure(Exception("Not logged in"))
                }
                val currentUserId = currentUser.uid

                val messageId = chatsRef.child(chatId).child("messages").push().key
                    ?: return@withContext Result.failure(Exception("Failed to generate message ID"))
                val timestamp = System.currentTimeMillis()

                val message = Message(
                    messageId = messageId,
                    chatId = chatId,
                    senderId = currentUserId,
                    receiverId = receiverId,
                    text = text,
                    timestamp = timestamp
                )

                chatsRef.child(chatId).child("messages").child(messageId).setValue(message).await()
                chatsRef.child(chatId).child("lastMessage").setValue(message).await()

                Result.success(Unit)

            } catch (e: Exception) {
                Log.e("MessageRepository", "Error sending message", e)
                Result.failure(e)
            }
        }
    }

    fun getMessagesForChat(chatId: String): Flow<List<Message>> = callbackFlow {
        val messagesList = mutableListOf<Message>()

        val listener = chatsRef.child(chatId).child("messages")
            .orderByChild("timestamp")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val message = snapshot.getValue<Message>()
                    message?.let {
                        messagesList.add(it)
                        messagesList.sortBy { msg -> msg.timestamp }
                        trySend(messagesList.toList())
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val message = snapshot.getValue<Message>()
                    message?.let {
                        val index = messagesList.indexOfFirst { msg -> msg.messageId == it.messageId }
                        if (index != -1) {
                            messagesList[index] = it
                            messagesList.sortBy { msg -> msg.timestamp }
                            trySend(messagesList.toList())
                        }
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val message = snapshot.getValue<Message>()
                    message?.let {
                        messagesList.removeAll { msg -> msg.messageId == it.messageId }
                        trySend(messagesList.toList())
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MessageRepository", "Error listening to messages", error.toException())
                    close(error.toException())
                }
            })

        awaitClose { chatsRef.child(chatId).child("messages").removeEventListener(listener) }
    }.flowOn(Dispatchers.IO)
}