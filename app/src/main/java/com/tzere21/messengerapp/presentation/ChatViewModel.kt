package com.tzere21.messengerapp.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tzere21.messengerapp.data.ChatRepository
import com.tzere21.messengerapp.domain.Message
import com.tzere21.messengerapp.domain.User
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val otherUser: User
) : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val _chatId = MutableLiveData<String>()

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> get() = _messages

    init {
        initializeChat()
    }

    private fun initializeChat() {
        _isLoading.value = true
        viewModelScope.launch {
            val result = chatRepository.getChatId(otherUser)
            _isLoading.value = false

            result.fold(
                onSuccess = { chatId ->
                    _chatId.value = chatId
                    viewModelScope.launch {
                        chatRepository.getMessagesForChat(chatId)
                            .catch { exception ->
                                _error.value = exception.message ?: "Error loading messages"
                            }
                            .collect { messagesList ->
                                _messages.value = messagesList
                            }
                    }
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to initialize chat"
                }
            )
        }
    }

    fun sendMessage(text: String) {
        val currentChatId = _chatId.value ?: return

        if (text.trim().isEmpty()) {
            _error.value = "Message cannot be empty"
            return
        }

        viewModelScope.launch {
            val result = chatRepository.sendMessage(currentChatId, otherUser, text.trim())

            result.fold(
                onSuccess = {},
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to send message"
                }
            )
        }
    }

    companion object {
        fun create(chatRepository: ChatRepository, otherUser: User): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChatViewModel(chatRepository, otherUser) as T
                }
            }
        }
    }
}