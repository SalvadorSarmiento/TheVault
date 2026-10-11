package com.example.thevault.ui.auth

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.thevault.BuildConfig
import com.example.thevault.data.auth.awaitResult
import com.google.firebase.auth.FirebaseAuth
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

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    var uiState by mutableStateOf(AuthUiState())
        private set

    init {
        viewModelScope.launch {
            delay(1_400)
            val hasFirebaseSession = BuildConfig.FIREBASE_CONFIGURED &&
                FirebaseAuth.getInstance().currentUser != null
            uiState = uiState.copy(
                showSplash = false,
                isAuthenticated = hasFirebaseSession
            )
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
            if (BuildConfig.FIREBASE_CONFIGURED) {
                runCatching {
                    FirebaseAuth.getInstance()
                        .signInWithEmailAndPassword(uiState.email.trim(), uiState.password)
                        .awaitResult()
                }.onSuccess {
                    uiState = uiState.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        password = ""
                    )
                }.onFailure { error ->
                    uiState = uiState.copy(
                        isLoading = false,
                        error = error.localizedMessage ?: "No se pudo iniciar sesión con Firebase."
                    )
                }
            } else {
                delay(350)
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
    }

    fun logout() {
        if (BuildConfig.FIREBASE_CONFIGURED) {
            FirebaseAuth.getInstance().signOut()
        }
        uiState = AuthUiState(showSplash = false)
    }

    companion object {
        const val DEMO_EMAIL = "ana@ejemplo.com"
        const val DEMO_PASSWORD = "123456"
    }
}
