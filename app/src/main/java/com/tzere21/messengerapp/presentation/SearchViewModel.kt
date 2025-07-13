package com.tzere21.messengerapp.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tzere21.messengerapp.data.UserRepository
import com.tzere21.messengerapp.domain.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> get() = _users

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> get() = _isEmpty

    private var searchJob: Job? = null
    private var currentQuery = ""
    private val allUsers = mutableListOf<User>()

    init {
        loadInitialUsers()
    }

    private fun loadInitialUsers() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            val result = userRepository.searchUsers("")
            _isLoading.value = false

            result.fold(
                onSuccess = { userList ->
                    allUsers.clear()
                    allUsers.addAll(userList)
                    _users.value = userList
                    _isEmpty.value = userList.isEmpty()
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to load users"
                    _isEmpty.value = true
                }
            )
        }
    }

    fun searchUsers(query: String) {
        currentQuery = query.trim()

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)

            _isLoading.value = true
            _error.value = null

            val result = userRepository.searchUsers(currentQuery)
            _isLoading.value = false

            result.fold(
                onSuccess = { userList ->
                    _users.value = userList
                    _isEmpty.value = userList.isEmpty()
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Search failed"
                    _isEmpty.value = true
                }
            )
        }
    }

    companion object {
        fun create(userRepository: UserRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SearchViewModel(userRepository) as T
                }
            }
        }
    }
}