package com.tzere21.messengerapp.data

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import com.tzere21.messengerapp.domain.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import androidx.core.net.toUri

class UserRepository(private val context: Context) {

    private val auth = Firebase.auth
    private val database = Firebase.database
    private val usersRef = database.getReference("users")

    fun getCurrentUserProfile(): Flow<User?> = callbackFlow {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = usersRef.child(currentUser.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue<User>()
                    trySend(user)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UserRepository", "Error loading user profile", error.toException())
                    close(error.toException())
                }
            })

        awaitClose { usersRef.child(currentUser.uid).removeEventListener(listener) }
    }.flowOn(Dispatchers.IO)

    suspend fun updateUserProfile(nickname: String, profession: String, photoPath: String? = null): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUser = auth.currentUser ?: return@withContext Result.failure(Exception("No user logged in"))

                val currentProfile = getCurrentUserProfileOnce()
                val currentNickname = currentProfile?.nickname ?: ""

                if (currentNickname != nickname) {
                    val nicknameExists = checkNicknameExists(nickname)
                    if (nicknameExists) {
                        return@withContext Result.failure(Exception("Nickname already exists"))
                    }
                }

                var photoUrl = currentProfile?.photoUrl ?: ""
                if (photoPath != null) {
                    val uploadResult = saveProfilePhotoLocally(photoPath)
                    if (uploadResult.isSuccess) {
                        photoUrl = uploadResult.getOrNull() ?: photoPath
                    }
                }

                val updates = mapOf(
                    "nickname" to nickname,
                    "email" to (currentProfile?.email ?: ""),
                    "profession" to profession,
                    "photoUrl" to photoUrl
                )

                usersRef.child(currentUser.uid).updateChildren(updates).await()
                Result.success(Unit)

            } catch (e: Exception) {
                Log.e("UserRepository", "Error updating profile", e)
                Result.failure(e)
            }
        }
    }

    private suspend fun saveProfilePhotoLocally(photoPath: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUser = auth.currentUser ?: return@withContext Result.failure(Exception("No user"))

                val inputStream = if (photoPath.startsWith("content://")) {
                    context.contentResolver.openInputStream(photoPath.toUri())
                } else {
                    File(photoPath).inputStream()
                }

                val destinationDir = File(context.filesDir, "profile_photos")
                if (!destinationDir.exists()) {
                    destinationDir.mkdirs()
                }

                val destinationFile = File(destinationDir, "${currentUser.uid}_profile.jpg")

                inputStream?.use { input ->
                    destinationFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                Result.success(destinationFile.absolutePath)

            } catch (e: Exception) {
                Log.e("UserRepository", "Error saving photo locally", e)
                Result.failure(e)
            }
        }
    }

    private suspend fun getCurrentUserProfileOnce(): User? {
        return withContext(Dispatchers.IO) {
            try {
                val currentUser = auth.currentUser ?: return@withContext null
                val snapshot = usersRef.child(currentUser.uid).get().await()
                snapshot.getValue<User>()
            } catch (e: Exception) {
                Log.e("UserRepository", "Error getting current user", e)
                null
            }
        }
    }

    private suspend fun checkNicknameExists(nickname: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = usersRef
                    .orderByChild("nickname")
                    .equalTo(nickname)
                    .get()
                    .await()
                snapshot.exists()
            } catch (e: Exception) {
                Log.e("UserRepository", "Error checking nickname", e)
                false
            }
        }
    }

    fun logout() {
        auth.signOut()
    }
}