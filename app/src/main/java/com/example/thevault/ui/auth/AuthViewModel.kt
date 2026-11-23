package com.example.thevault.ui.auth

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.thevault.data.auth.LocalAccessService
import com.example.thevault.data.auth.LocalAccountStore
import com.example.thevault.data.auth.LocalSession
import com.example.thevault.data.auth.PendingAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException

enum class AuthStage {
    WELCOME, PRIVACY, KEY_EXPLANATION, LOGIN, REGISTER, REGISTER_PASSWORD,
    IMPORT_LEGACY, RECOVERY_KEY, CONFIRM_KEY, RECOVER, RESET_PASSWORD
}

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
                uiState = uiState.copy(showSplash = false, stage = if (exists) AuthStage.LOGIN else AuthStage.WELCOME,
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
        if (uiState.isLoading || stage !in setOf(AuthStage.WELCOME, AuthStage.LOGIN, AuthStage.REGISTER, AuthStage.RECOVER)) return
        clearPending()
        uiState = AuthUiState(showSplash = false, stage = stage, email = uiState.email,
            hasLegacyVault = uiState.hasLegacyVault, importLegacy = uiState.hasLegacyVault)
    }

    fun back() {
        if (uiState.isLoading) return
        when (uiState.stage) {
            AuthStage.WELCOME, AuthStage.LOGIN -> return
            AuthStage.PRIVACY -> moveTo(AuthStage.WELCOME)
            AuthStage.KEY_EXPLANATION -> moveTo(AuthStage.PRIVACY)
            AuthStage.REGISTER -> moveTo(AuthStage.KEY_EXPLANATION)
            AuthStage.REGISTER_PASSWORD -> moveTo(AuthStage.REGISTER)
            AuthStage.IMPORT_LEGACY -> moveTo(AuthStage.REGISTER_PASSWORD)
            AuthStage.RECOVERY_KEY -> {
                clearPending()
                uiState = uiState.copy(stage = AuthStage.REGISTER_PASSWORD, recoveryCode = "", recoveryInput = "",
                    keyNoted = false, showPassword = false, error = null)
            }
            AuthStage.CONFIRM_KEY -> moveTo(AuthStage.RECOVERY_KEY)
            AuthStage.RECOVER -> changeStage(AuthStage.LOGIN)
            AuthStage.RESET_PASSWORD -> uiState = uiState.copy(stage = AuthStage.RECOVER,
                password = "", confirmation = "", error = null)
        }
    }

    private fun moveTo(stage: AuthStage) { uiState = uiState.copy(stage = stage, error = null, showPassword = false) }

    private fun validEmail(): Boolean = try {
        uiState = uiState.copy(email = LocalAccountStore.normalizeEmail(uiState.email), error = null)
        true
    } catch (error: IllegalArgumentException) { uiState = uiState.copy(error = error.message); false }

    private fun validPassword(): Boolean {
        if (!matchingPassword()) return false
        val password = uiState.password.toCharArray()
        return try { LocalAccountStore.requirePassword(password); true }
        catch (error: IllegalArgumentException) { uiState = uiState.copy(error = error.message); false }
        finally { password.fill('\u0000') }
    }

    fun login() {
        if (uiState.isLoading) return
        when (uiState.stage) {
            AuthStage.WELCOME -> moveTo(AuthStage.PRIVACY)
            AuthStage.PRIVACY -> moveTo(AuthStage.KEY_EXPLANATION)
            AuthStage.KEY_EXPLANATION -> moveTo(AuthStage.REGISTER)
            AuthStage.REGISTER -> if (validEmail()) moveTo(AuthStage.REGISTER_PASSWORD)
            AuthStage.REGISTER_PASSWORD -> if (validPassword()) {
                if (uiState.hasLegacyVault) moveTo(AuthStage.IMPORT_LEGACY) else register()
            }
            AuthStage.IMPORT_LEGACY -> register()
            AuthStage.RECOVERY_KEY -> moveTo(AuthStage.CONFIRM_KEY)
            AuthStage.CONFIRM_KEY -> confirmKey()
            AuthStage.RECOVER -> if (validEmail()) {
                val code = uiState.recoveryInput.filterNot { it.isWhitespace() || it == '-' }
                if (code.length == 64 && code.all { it.uppercaseChar() in "0123456789ABCDEF" }) moveTo(AuthStage.RESET_PASSWORD)
                else uiState = uiState.copy(error = "Escribe la clave de recuperación completa.")
            }
            AuthStage.RESET_PASSWORD -> recover()
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
        if (!validPassword()) return
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
        if (!validPassword()) return
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
