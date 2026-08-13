package com.pumpwatch.app.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pumpwatch.app.data.repository.OwnerAuthStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class AuthScreenState { LOADING, NEEDS_SETUP, NEEDS_LOGIN, UNLOCKED }

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val store = OwnerAuthStore(application)

    private val _screenState = MutableStateFlow(AuthScreenState.LOADING)
    val screenState: StateFlow<AuthScreenState> = _screenState

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        viewModelScope.launch {
            _screenState.value = if (store.hasCredentials()) {
                AuthScreenState.NEEDS_LOGIN
            } else {
                AuthScreenState.NEEDS_SETUP
            }
        }
    }

    fun createCredentials(username: String, password: String, confirmPassword: String) {
        when {
            username.isBlank() -> _error.value = "نام کاربری را وارد کن"
            password.length < 6 -> _error.value = "رمز عبور باید حداقل ۶ کاراکتر باشد"
            password != confirmPassword -> _error.value = "تکرار رمز عبور مطابقت ندارد"
            else -> viewModelScope.launch {
                store.setCredentials(username, password)
                _error.value = null
                _screenState.value = AuthScreenState.UNLOCKED
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            if (store.verify(username, password)) {
                _error.value = null
                _screenState.value = AuthScreenState.UNLOCKED
            } else {
                _error.value = "نام کاربری یا رمز عبور اشتباه است"
            }
        }
    }

    fun lock() {
        _screenState.value = AuthScreenState.NEEDS_LOGIN
    }
}
