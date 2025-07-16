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

        val userChatMap = mutableMapOf<String, UserChat>()
        val listener = userChatsRef.child(currentUserId)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val userChat = snapshot.getValue<UserChat>()
                    userChat?.let {
                        userChatMap[it.chatId] = it
                        emitFilteredList(userChatMap, query)
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val userChat = snapshot.getValue<UserChat>()
                    userChat?.let {
                        userChatMap[it.chatId] = it
                        emitFilteredList(userChatMap, query)
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val userChat = snapshot.getValue<UserChat>()
                    userChat?.let {
                        userChatMap.remove(it.chatId)
                        emitFilteredList(userChatMap, query)
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UserChatRepository", "Error listening to user chats", error.toException())
                    close(error.toException())
                }

                private fun emitFilteredList(chatMap: Map<String, UserChat>, searchQuery: String) {
                    val filteredList = chatMap.values
                        .filter { chat ->
                            chat.timestamp != 0L &&
                                    (searchQuery.isEmpty() ||
                                            chat.secondUserName.startsWith(searchQuery))
                        }
                        .sortedByDescending { it.timestamp }

                    trySend(filteredList)
                }
            })

        awaitClose { userChatsRef.child(currentUserId).removeEventListener(listener) }
    }.flowOn(Dispatchers.IO)
}