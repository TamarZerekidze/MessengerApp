package com.tzere21.messengerapp.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import com.tzere21.messengerapp.domain.UserChat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

class UserChatRepository {

    private val auth = Firebase.auth
    private val database = Firebase.database
    private val userChatsRef = database.getReference("userChats")

    fun getUserChats(query: String): Flow<List<UserChat>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            close(Exception("Not logged in"))
            return@callbackFlow
        }

        val userChatList = mutableListOf<UserChat>()
        val listener = userChatsRef.child(currentUserId)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val userChat = snapshot.getValue<UserChat>()
                    userChat?.let {
                        if (it.timestamp != 0L && it.secondUserName.startsWith(query)) {
                            userChatList.add(it)
                            userChatList.sortBy { chat -> -chat.timestamp }
                            trySend(userChatList.toList())
                        }
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val userChat = snapshot.getValue<UserChat>()
                    userChat?.let {
                        val index = userChatList.indexOfFirst { chat -> chat.chatId == it.chatId }
                        if (index != -1) {
                            userChatList[index] = it
                            userChatList.sortBy { chat -> -chat.timestamp }
                            trySend(userChatList.toList())
                        }
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val userChat = snapshot.getValue<UserChat>()
                    userChat?.let {
                        userChatList.removeAll { chat -> chat.chatId == it.chatId }
                        trySend(userChatList.toList())
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UserChatRepository", "Error listening to user chats", error.toException())
                    close(error.toException())
                }
            })

        awaitClose { userChatsRef.child(currentUserId).removeEventListener(listener) }
    }.flowOn(Dispatchers.IO)
}