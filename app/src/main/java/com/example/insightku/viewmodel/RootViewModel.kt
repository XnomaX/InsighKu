
package com.example.insightku.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RootViewModel : ViewModel() {
    private val _globalError = MutableStateFlow<String?>(null)
    val globalError = _globalError.asStateFlow()

    fun setGlobalError(message: String?) {
        _globalError.update { message }
    }
}
