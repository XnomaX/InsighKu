package com.example.insightku.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insightku.data.model.UserData
import com.example.insightku.utils.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _userState = MutableStateFlow(UserData())
    val userState: StateFlow<UserData> = _userState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            try {
                val isLoggedIn = sessionManager.getLoginStatus()
                _isLoggedIn.value = isLoggedIn

                if (isLoggedIn) {
                    val (email, name, userId) = sessionManager.getUserData()
                    _userState.value = UserData(
                        email = email,
                        name = name,
                        token = userId // menggunakan userId sebagai token
                    )
                }
            } catch (e: Exception) {
                // Handle error jika tidak bisa load user data
                _isLoggedIn.value = false
                _userState.value = UserData()
            }
        }
    }

    suspend fun logout() {
        try {
            sessionManager.clearSession()
            _isLoggedIn.value = false
            _userState.value = UserData()
        } catch (e: Exception) {
            // Handle logout error
        }
    }
}
