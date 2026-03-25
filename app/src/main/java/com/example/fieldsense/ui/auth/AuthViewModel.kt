package com.example.fieldsense.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fieldsense.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        if (repository.isUserLoggedIn()) {
            _authState.value = AuthState.Authenticated
        }
    }

    fun getUserEmail(): String = repository.getCurrentUserEmail()

    fun signOut() {
        repository.signOut()
        _authState.value = AuthState.Idle
    }

    fun authenticateWithEmail(email: String, pass: String, isLogin: Boolean) {
        if (email.isEmpty() || pass.isEmpty()) {
            _authState.value = AuthState.Error("Please fill in all fields")
            return
        }

        _authState.value = AuthState.Loading

        viewModelScope.launch {
            val result = if (isLogin) {
                repository.signInWithEmail(email, pass)
            } else {
                repository.signUpWithEmail(email, pass)
            }

            result.onSuccess {
                _authState.value = AuthState.Authenticated
            }
            result.onFailure { exception ->
                _authState.value = AuthState.Error(exception.message ?: "Authentication failed")
            }
        }
    }

    fun authenticateWithGoogle(idToken: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = repository.signInWithGoogle(idToken)
            result.onSuccess {
                _authState.value = AuthState.Authenticated
            }
            result.onFailure { exception ->
                _authState.value = AuthState.Error(exception.message ?: "Google Sign-In failed")
            }
        }
    }
}

class AuthViewModelFactory(private val repository: AuthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}