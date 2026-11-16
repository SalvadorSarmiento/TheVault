package com.example.thevault.ui.auth

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.thevault.data.auth.LocalAccessService
import com.example.thevault.data.auth.LocalSession
import com.example.thevault.data.auth.PendingAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException

enum class AuthStage { LOGIN, REGISTER, RECOVERY_KEY, RECOVER }

data class AuthUiState(
    val showSplash: Boolean = true,
    val stage: AuthStage = AuthStage.LOGIN,
    val email: String = "",
    val password: String = "",
    val confirmation: String = "",
    val recoveryCode: String = "",
    val recoveryInput: String = "",
    val keyNoted: Boolean = false,
    val hasLegacyVault: Boolean = false,
    val importLegacy: Boolean = false,
    val oldPassword: String = "",
    val accountId: String? = null,
    val showPassword: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false
) {
    override fun toString() = "AuthUiState(stage=$stage, loading=$isLoading)"
}

class AuthViewModel @JvmOverloads constructor(
    application: Application,
    private val access: LocalAccessService = LocalAccessService(application)
) : AndroidViewModel(application) {
    private var pendingAccount: PendingAccount? = null
    private var pendingSession: LocalSession? = null
    private var legacyPassword: CharArray? = null
    var uiState by mutableStateOf(AuthUiState())
        private set

    init {
        viewModelScope.launch {
            try {
                val (exists, legacy) = withContext(Dispatchers.IO) {
                    access.accounts.hasAccounts() to access.hasLegacyVault()
                }
                delay(1_000)
                uiState = uiState.copy(showSplash = false, stage = if (exists) AuthStage.LOGIN else AuthStage.REGISTER,
                    hasLegacyVault = legacy, importLegacy = legacy)
            } catch (_: Exception) {
                uiState = uiState.copy(showSplash = false, error = "No se pudo leer el acceso local. No borres los datos de la app.")
            }
        }
    }

    fun onEmailChange(value: String) { if (!uiState.isLoading) uiState = uiState.copy(email = value.take(254), error = null) }
    fun onPasswordChange(value: String) { if (!uiState.isLoading) uiState = uiState.copy(password = value.take(256), error = null) }
    fun onConfirmationChange(value: String) { if (!uiState.isLoading) uiState = uiState.copy(confirmation = value.take(256), error = null) }
    fun onRecoveryChange(value: String) { if (!uiState.isLoading) uiState = uiState.copy(recoveryInput = value.take(100), error = null) }
    fun onOldPasswordChange(value: String) { if (!uiState.isLoading) uiState = uiState.copy(oldPassword = value.take(256), error = null) }
    fun onImportChange(value: Boolean) { if (!uiState.isLoading) uiState = uiState.copy(importLegacy = value, error = null) }
    fun onKeyNoted(value: Boolean) { if (!uiState.isLoading) uiState = uiState.copy(keyNoted = value, error = null) }
    fun togglePasswordVisibility() { uiState = uiState.copy(showPassword = !uiState.showPassword) }

    fun changeStage(stage: AuthStage) {
        if (uiState.isLoading) return
        clearPending()
        uiState = AuthUiState(showSplash = false, stage = stage, email = uiState.email,
            hasLegacyVault = uiState.hasLegacyVault, importLegacy = uiState.hasLegacyVault)
    }

    fun login() {
        if (uiState.isLoading) return
        when (uiState.stage) {
            AuthStage.REGISTER -> register()
            AuthStage.RECOVERY_KEY -> confirmKey()
            AuthStage.RECOVER -> recover()
            AuthStage.LOGIN -> {
                val email = uiState.email
                val password = uiState.password.toCharArray()
                work {
                    try { authenticated(withContext(Dispatchers.IO) { access.login(email, password) }) }
                    finally { password.fill('\u0000') }
                }
            }
        }
    }

    private fun matchingPassword(): Boolean {
        if (uiState.password != uiState.confirmation) {
            uiState = uiState.copy(error = "Las contraseñas no coinciden.")
            return false
        }
        return true
    }

    private fun register() {
        if (!matchingPassword()) return
        if (uiState.importLegacy && uiState.oldPassword.isBlank()) {
            uiState = uiState.copy(error = "Escribe la contraseña de la bóveda anterior para conservarla.")
            return
        }
        val email = uiState.email
        val password = uiState.password.toCharArray()
        legacyPassword?.fill('\u0000')
        legacyPassword = uiState.oldPassword.takeIf { uiState.importLegacy }?.toCharArray()
        work {
            try {
                val pending = withContext(Dispatchers.IO) { access.accounts.prepare(email, password) }
                pendingAccount = pending
                uiState = uiState.copy(stage = AuthStage.RECOVERY_KEY, email = pending.email,
                    recoveryCode = pending.recoveryCode, password = "", confirmation = "", oldPassword = "")
            } finally { password.fill('\u0000') }
        }
    }

    private fun confirmKey() {
        val pending = pendingAccount ?: return
        if (!uiState.keyNoted || uiState.recoveryInput.trim().uppercase() != pending.recoveryCode.takeLast(8)) {
            uiState = uiState.copy(error = "Confirma que la anotaste y escribe los últimos 8 caracteres de la clave.")
            return
        }
        work {
            val session = withContext(Dispatchers.IO) { access.complete(pending, legacyPassword) }
            val legacyRemaining = withContext(Dispatchers.IO) { access.hasLegacyVault() }
            uiState = uiState.copy(hasLegacyVault = legacyRemaining)
            pendingAccount = null
            legacyPassword?.fill('\u0000'); legacyPassword = null
            authenticated(session)
        }
    }

    private fun recover() {
        if (!matchingPassword()) return
        val email = uiState.email
        val recovery = uiState.recoveryInput
        val password = uiState.password.toCharArray()
        work {
            try { authenticated(withContext(Dispatchers.IO) { access.recover(email, recovery, password) }) }
            finally { password.fill('\u0000') }
        }
    }

    private fun work(block: suspend () -> Unit) {
        uiState = uiState.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                uiState = uiState.copy(error = if (error is IllegalArgumentException || error is IllegalStateException)
                    error.message ?: "No se pudo completar el acceso." else "No se pudo abrir o guardar la cuenta local.")
            } finally { uiState = uiState.copy(isLoading = false) }
        }
    }

    private fun authenticated(session: LocalSession) {
        pendingSession?.close()
        pendingSession = session
        uiState = AuthUiState(showSplash = false, email = uiState.email,
            accountId = session.accountId, isAuthenticated = true, hasLegacyVault = uiState.hasLegacyVault)
    }

    fun consumeVaultPassword(): CharArray? = pendingSession?.let { session ->
        session.vaultPassword.copyOf().also { session.close(); pendingSession = null }
    }
    fun logout() { clearPending(); uiState = AuthUiState(showSplash = false, email = uiState.email, hasLegacyVault = uiState.hasLegacyVault) }
    fun rejectVaultUnlock(message: String) { logout(); uiState = uiState.copy(error = message) }
    private fun clearPending() {
        pendingAccount?.close(); pendingAccount = null
        pendingSession?.close(); pendingSession = null
        legacyPassword?.fill('\u0000'); legacyPassword = null
    }
    override fun onCleared() { clearPending(); super.onCleared() }
}
