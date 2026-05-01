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
                        token = userId
                    )
                }
            } catch (e: Exception) {
                _isLoggedIn.value = false
                _userState.value = UserData()
            }
        }
    }

    // BUG3 FIX: Fungsi logout() yang lama DIHAPUS karena hanya clear DataStore
    // tanpa Firebase SignOut → security risk (token masih aktif).
    // Logout yang benar ada di SettingsViewModel via LogoutUseCase yang melakukan:
    //   1. firebaseAuth.signOut()
    //   2. sessionManager.clearSession()
    //   3. Hapus cache Room (transaksi + kategori)
    // Jangan tambah fungsi logout() di sini untuk menghindari duplikasi yang salah.
}

