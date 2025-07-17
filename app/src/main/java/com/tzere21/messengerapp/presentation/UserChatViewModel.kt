package com.tzere21.messengerapp.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tzere21.messengerapp.data.UserChatRepository
import com.tzere21.messengerapp.domain.UserChat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class UserChatViewModel(
    private val userChatRepository: UserChatRepository
) : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val _userChats = MutableLiveData<List<UserChat>>()
    val userChats: LiveData<List<UserChat>> get() = _userChats

    init {
        initializeUserChats()
    }

    private fun initializeUserChats() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            userChatRepository.getUserChats("")
                .onStart {
                    launch {
                        delay(2000)
                        if (_isLoading.value == true) {
                            _isLoading.value = false
                            _userChats.value = emptyList()
                        }
                    }
                }
                .catch { exception ->
                    _isLoading.value = false
                    _error.value = exception.message ?: "Error loading messages"
                }
                .collect { userChatList ->
                    _isLoading.value = false
                    _userChats.value = userChatList
                }
        }
    }

    fun searchUserChats(query: String) {
        _isLoading.value = true
        viewModelScope.launch {
            userChatRepository.getUserChats(query)
                .onStart {
                    launch {
                        delay(2000)
                        if (_isLoading.value == true) {
                            _isLoading.value = false
                            _userChats.value = emptyList()
                        }
                    }
                }
                .catch { exception ->
                    _isLoading.value = false
                    _error.value = exception.message ?: "Error loading messages"
                }
                .collect { userChatList ->
                    _isLoading.value = false
                    _userChats.value = userChatList
                }
        }
    }

    companion object {
        fun create(userChatRepository: UserChatRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return UserChatViewModel(userChatRepository) as T
                }
            }
        }
    }
}