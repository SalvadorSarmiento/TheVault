package com.example.thevault.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class AuthUiState(
    val showSplash: Boolean = true,
    val email: String = "",
    val password: String = "",
    val showPassword: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false
)

class AuthViewModel : ViewModel() {
    var uiState by mutableStateOf(AuthUiState())
        private set

    init {
        viewModelScope.launch {
            delay(1_400)
            uiState = uiState.copy(showSplash = false)
        }
    }

    fun onEmailChange(value: String) {
        uiState = uiState.copy(email = value, error = null)
    }

    fun onPasswordChange(value: String) {
        uiState = uiState.copy(password = value, error = null)
    }

    fun togglePasswordVisibility() {
        uiState = uiState.copy(showPassword = !uiState.showPassword)
    }

    fun login() {
        if (uiState.email.isBlank() || uiState.password.isBlank()) {
            uiState = uiState.copy(error = "Completa el correo y la contraseña.")
            return
        }

        uiState = uiState.copy(isLoading = true, error = null)
        viewModelScope.launch {
            delay(650)
            val isValid = uiState.email.trim().equals(DEMO_EMAIL, ignoreCase = true) &&
                uiState.password == DEMO_PASSWORD
            uiState = if (isValid) {
                uiState.copy(isLoading = false, isAuthenticated = true, password = "")
            } else {
                uiState.copy(
                    isLoading = false,
                    error = "Correo o contraseña incorrectos. Usa el acceso de demostración."
                )
            }
        }
    }

    fun logout() {
        uiState = AuthUiState(showSplash = false)
    }

    companion object {
        const val DEMO_EMAIL = "ana@ejemplo.com"
        const val DEMO_PASSWORD = "123456"
    }
}
