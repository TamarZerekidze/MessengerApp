package com.tzere21.messengerapp.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.tzere21.messengerapp.data.UserRepository
import com.tzere21.messengerapp.domain.User
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ProfileViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _updateResult = MutableLiveData<Result<String>>()
    val updateResult: LiveData<Result<String>> get() = _updateResult

    val userProfile: LiveData<User?> = userRepository.getCurrentUserProfile()
        .catch { emit(null) }
        .asLiveData()

    fun updateProfile(nickname: String, profession: String, photoPath: String? = null) {
        if (nickname.isBlank() || profession.isBlank()) {
            _updateResult.value = Result.failure(Exception("All fields are required"))
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            val result = userRepository.updateUserProfile(nickname, profession, photoPath)
            _isLoading.value = false

            if (result.isSuccess) {
                _updateResult.value = Result.success("Profile updated successfully")
            } else {
                _updateResult.value = Result.failure(result.exceptionOrNull() ?: Exception("Update failed"))
            }
        }
    }

    fun logout() {
        userRepository.logout()
    }

    companion object {
        fun create(userRepository: UserRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ProfileViewModel(userRepository) as T
                }
            }
        }
    }
}