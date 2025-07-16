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
import com.tzere21.messengerapp.domain.User
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
    private val usersRef = database.getReference("users")

    suspend fun getChatId(otherUser: User): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUserAuth = auth.currentUser
                if (currentUserAuth == null) {
                    return@withContext Result.failure(Exception("Not logged in"))
                }
                val currentUserId = currentUserAuth.uid

                val chatsSnapshot = chatsRef.get().await()

                var chatId = ""
                for (chatSnapshot in chatsSnapshot.children) {
                    val participants = chatSnapshot.child("participants").getValue<List<String>>()

                    if (participants != null &&
                        participants.contains(currentUserId) &&
                        participants.contains(otherUser.uid)) {

                        val key = chatSnapshot.key
                        if (key != null) {
                            chatId = key
                            break
                        }
                    }
                }

                if (chatId.isEmpty()) {
                    val currentUserData = getCurrentUser(currentUserId)
                    if (currentUserData != null) {
                        chatId = createNewChat(currentUserData, otherUser)
                    } else {
                        return@withContext Result.failure(Exception("Could not load current user data"))
                    }
                }
                Result.success(chatId)

            } catch (e: Exception) {
                Log.e("ChatRepository", "Error getting/creating chat", e)
                Result.failure(e)
            }
        }
    }

    private suspend fun getCurrentUser(userId: String): User? {
        return try {
            val snapshot = usersRef.child(userId).get().await()
            snapshot.getValue<User>()
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error getting current user", e)
            null
        }
    }

    private suspend fun createNewChat(firstUser: User, secondUser: User): String {
        val chatId = chatsRef.push().key ?: throw Exception("Failed to generate chat ID")

        val chatData = mapOf(
            "chatId" to chatId,
            "participants" to listOf(firstUser.uid, secondUser.uid),
            "lastMessage" to null
        )

        val userChatForFirst = UserChat(
            chatId = chatId,
            secondUserId = secondUser.uid,
            secondUserName = secondUser.nickname,
            secondUserEmail = secondUser.email,
            secondUserProfession = secondUser.profession,
            secondUserPhotoUrl = secondUser.photoUrl,
            lastMessage = "",
            timestamp = 0L
        )
        val userChatForSecond = UserChat(
            chatId = chatId,
            secondUserId = firstUser.uid,
            secondUserName = firstUser.nickname,
            secondUserEmail = firstUser.email,
            secondUserProfession = firstUser.profession,
            secondUserPhotoUrl = firstUser.photoUrl,
            lastMessage = "",
            timestamp = 0L
        )

        chatsRef.child(chatId).setValue(chatData).await()
        userChatsRef.child(firstUser.uid).child(chatId).setValue(userChatForFirst).await()
        userChatsRef.child(secondUser.uid).child(chatId).setValue(userChatForSecond).await()

        return chatId
    }

    suspend fun sendMessage(chatId: String, receiverUser: User, text: String): Result<Unit> {
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
                    receiverId = receiverUser.uid,
                    text = text,
                    timestamp = timestamp
                )

                chatsRef.child(chatId).child("messages").child(messageId).setValue(message).await()
                chatsRef.child(chatId).child("lastMessage").setValue(message).await()

                val chatSnapshot = chatsRef.child(chatId).get().await()
                val participants = chatSnapshot.child("participants").getValue<List<String>>()

                participants?.forEach { userId ->
                    userChatsRef.child(userId).child(chatId).child("lastMessage").setValue(text).await()
                    userChatsRef.child(userId).child(chatId).child("timestamp").setValue(timestamp).await()
                }

                Result.success(Unit)

            } catch (e: Exception) {
                Log.e("ChatRepository", "Error sending message", e)
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
                    Log.e("ChatRepository", "Error listening to messages", error.toException())
                    close(error.toException())
                }
            })

        awaitClose { chatsRef.child(chatId).child("messages").removeEventListener(listener) }
    }.flowOn(Dispatchers.IO)
}