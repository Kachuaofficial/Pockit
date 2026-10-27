package com.invatech.pockit.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invatech.pockit.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isCheckingAuth: Boolean = true,
    val isSignedIn: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(
            isCheckingAuth = true,
            isSignedIn = authRepository.isUserLoggedIn()
        )
    )

    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkAuth()
    }

    private fun checkAuth() {
        viewModelScope.launch {
            val loggedIn = authRepository.isUserLoggedIn()
            _uiState.value = _uiState.value.copy(
                isCheckingAuth = false,
                isSignedIn = loggedIn
            )
        }
    }

    fun signInWithGoogleToken(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            val credential =
                authRepository.createGoogleCredential(idToken)

            val result =
                authRepository.signInWithGoogleCredential(credential)

            _uiState.value = result.fold(
                onSuccess = {
                    AuthUiState(
                        isLoading = false,
                        isCheckingAuth = false,
                        isSignedIn = true
                    )
                },
                onFailure = { exception ->
                    AuthUiState(
                        isLoading = false,
                        isCheckingAuth = false,
                        isSignedIn = false,
                        errorMessage = exception.localizedMessage
                            ?: "Google sign-in failed. Please try again."
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null
        )
    }

    fun signOut() {
        authRepository.signOut()
        _uiState.value = AuthUiState(
            isLoading = false,
            isCheckingAuth = false,
            isSignedIn = false
        )
    }
}
