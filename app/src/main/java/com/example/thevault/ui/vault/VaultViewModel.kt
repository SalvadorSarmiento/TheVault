package com.example.thevault.ui.vault

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.thevault.BuildConfig
import com.example.thevault.data.auth.DevelopmentTokenProvider
import com.example.thevault.data.auth.FirebaseIdTokenProvider
import com.example.thevault.data.model.Credential
import com.example.thevault.data.model.CredentialCategory
import com.example.thevault.data.remote.HttpRemoteVaultRepository
import com.example.thevault.data.remote.RemoteVaultRepository
import com.example.thevault.data.repository.RoomVaultRepository
import com.example.thevault.data.repository.VaultRepository
import com.example.thevault.data.security.MigratingCredentialCipher
import com.google.firebase.auth.FirebaseAuth
import java.security.GeneralSecurityException
import kotlinx.coroutines.launch

enum class VaultDestination { VAULT, GENERATOR, CATEGORIES, SETTINGS, DETAIL, EDITOR }
enum class VaultFilter(val label: String) { ALL("Todas"), FAVORITES("Favoritos"), WORK("Trabajo") }

data class CredentialDraft(
    val id: Long? = null,
    val name: String = "",
    val username: String = "",
    val password: String = "",
    val website: String = "",
    val notes: String = "",
    val category: CredentialCategory = CredentialCategory.PERSONAL,
    val isFavorite: Boolean = false,
    val showPassword: Boolean = false,
    val submitted: Boolean = false
)

data class VaultUiState(
    val destination: VaultDestination = VaultDestination.VAULT,
    val credentials: List<Credential> = emptyList(),
    val query: String = "",
    val filter: VaultFilter = VaultFilter.ALL,
    val categoryFilter: CredentialCategory? = null,
    val selectedCredentialId: Long? = null,
    val draft: CredentialDraft = CredentialDraft(),
    val showDeleteDialog: Boolean = false,
    val darkTheme: Boolean = false,
    val generatedPassword: String = "vQ7!mR2#sL9@pX4$",
    val generatorLength: Int = 16,
    val includeUppercase: Boolean = true,
    val includeLowercase: Boolean = true,
    val includeNumbers: Boolean = true,
    val includeSymbols: Boolean = true,
    val isUnlocked: Boolean = false,
    val isUnlocking: Boolean = false,
    val isSyncing: Boolean = false,
    val message: String? = null
)

class VaultViewModel(
    application: Application
) : AndroidViewModel(application) {

    private var repository: VaultRepository? = null
    private var remoteRepository: RemoteVaultRepository? = null
    private var crypto: MigratingCredentialCipher? = null
    private var sessionVersion = 0L

    var uiState by mutableStateOf(VaultUiState())
        private set

    fun unlock(masterPassword: CharArray, onFailure: (String) -> Unit) {
        val version = ++sessionVersion
        val app = getApplication<Application>()
        crypto?.close()
        val sessionCrypto = MigratingCredentialCipher(app, masterPassword)
        masterPassword.fill('\u0000')
        crypto = sessionCrypto

        val localRepository = RoomVaultRepository(
            context = app,
            ownerIdProvider = ::currentOwnerId,
            crypto = sessionCrypto
        )
        val httpRepository = HttpRemoteVaultRepository(
            baseUrl = BuildConfig.API_BASE_URL,
            tokenProvider = if (BuildConfig.FIREBASE_CONFIGURED) {
                FirebaseIdTokenProvider()
            } else {
                DevelopmentTokenProvider()
            },
            crypto = sessionCrypto
        )
        repository = localRepository
        remoteRepository = httpRepository
        uiState = uiState.copy(isUnlocking = true, message = null)

        viewModelScope.launch {
            val localCredentials = runCatching { localRepository.getCredentials() }
                .getOrElse { error ->
                    failUnlock(version, error, onFailure)
                    return@launch
                }
            if (version != sessionVersion) return@launch

            uiState = uiState.copy(
                credentials = localCredentials,
                isUnlocked = true,
                isUnlocking = false
            )
            syncFromRemote(version, onFailure)
        }
    }

    val visibleCredentials: List<Credential>
        get() = uiState.credentials.filter { credential ->
            val matchesQuery = uiState.query.isBlank() ||
                credential.name.contains(uiState.query, ignoreCase = true) ||
                credential.username.contains(uiState.query, ignoreCase = true)
            val matchesFilter = when (uiState.filter) {
                VaultFilter.ALL -> true
                VaultFilter.FAVORITES -> credential.isFavorite
                VaultFilter.WORK -> credential.category == CredentialCategory.WORK
            }
            val matchesCategory = uiState.categoryFilter == null || credential.category == uiState.categoryFilter
            matchesQuery && matchesFilter && matchesCategory
        }

    val selectedCredential: Credential?
        get() = uiState.credentials.firstOrNull { it.id == uiState.selectedCredentialId }

    fun navigate(destination: VaultDestination) {
        uiState = if (destination == VaultDestination.VAULT) {
            uiState.copy(
                destination = destination,
                message = null,
                categoryFilter = null,
                filter = VaultFilter.ALL
            )
        } else {
            uiState.copy(destination = destination, message = null)
        }
    }

    fun onBack() {
        uiState = when (uiState.destination) {
            VaultDestination.DETAIL, VaultDestination.EDITOR -> uiState.copy(destination = VaultDestination.VAULT)
            else -> uiState.copy(destination = VaultDestination.VAULT)
        }
    }

    fun onQueryChange(value: String) {
        uiState = uiState.copy(query = value)
    }

    fun setFilter(filter: VaultFilter) {
        uiState = uiState.copy(filter = filter, categoryFilter = null)
    }

    fun openCategory(category: CredentialCategory) {
        uiState = uiState.copy(
            destination = VaultDestination.VAULT,
            categoryFilter = category,
            filter = VaultFilter.ALL,
            query = ""
        )
    }

    fun openCredential(id: Long) {
        uiState = uiState.copy(selectedCredentialId = id, destination = VaultDestination.DETAIL)
    }

    fun startCreate() {
        uiState = uiState.copy(draft = CredentialDraft(), destination = VaultDestination.EDITOR)
    }

    fun startEdit() {
        val credential = selectedCredential ?: return
        uiState = uiState.copy(
            draft = CredentialDraft(
                id = credential.id,
                name = credential.name,
                username = credential.username,
                password = credential.password,
                website = credential.website,
                notes = credential.notes,
                category = credential.category,
                isFavorite = credential.isFavorite
            ),
            destination = VaultDestination.EDITOR
        )
    }

    fun updateDraft(transform: (CredentialDraft) -> CredentialDraft) {
        uiState = uiState.copy(draft = transform(uiState.draft))
    }

    fun saveDraft() {
        val draft = uiState.draft.copy(submitted = true)
        uiState = uiState.copy(draft = draft)
        if (draft.name.isBlank() || draft.username.isBlank() || draft.password.isBlank()) return

        val current = draft.id?.let { id -> uiState.credentials.firstOrNull { it.id == id } }
        val credential = Credential(
            id = draft.id ?: 0L,
            remoteId = current?.remoteId,
            name = draft.name.trim(),
            username = draft.username.trim(),
            password = draft.password,
            website = draft.website.trim(),
            notes = draft.notes.trim(),
            category = draft.category,
            isFavorite = draft.isFavorite
        )
        val localRepository = repository ?: return
        val version = sessionVersion
        viewModelScope.launch {
            runCatching {
                val saved = localRepository.saveCredential(credential)
                if (version != sessionVersion) return@launch
                uiState = uiState.copy(
                    credentials = localRepository.getCredentials(),
                    selectedCredentialId = saved.id,
                    destination = VaultDestination.DETAIL,
                    message = if (draft.id == null) "Credencial agregada" else "Cambios guardados"
                )
                synchronizeCredential(saved, version)
            }.onFailure { error ->
                if (version == sessionVersion) {
                    uiState = uiState.copy(message = error.userMessage("No se pudo guardar la credencial"))
                }
            }
        }
    }

    fun toggleFavorite() {
        val credential = selectedCredential ?: return
        val updated = credential.copy(isFavorite = !credential.isFavorite)
        val localRepository = repository ?: return
        val version = sessionVersion
        viewModelScope.launch {
            runCatching {
                localRepository.saveCredential(updated)
                if (version != sessionVersion) return@launch
                uiState = uiState.copy(credentials = localRepository.getCredentials())
                synchronizeCredential(updated, version)
            }.onFailure { error ->
                if (version == sessionVersion) {
                    uiState = uiState.copy(message = error.userMessage("No se pudo actualizar"))
                }
            }
        }
    }

    fun requestDelete() {
        uiState = uiState.copy(showDeleteDialog = true)
    }

    fun dismissDelete() {
        uiState = uiState.copy(showDeleteDialog = false)
    }

    fun deleteSelected() {
        val credential = selectedCredential ?: return
        val localRepository = repository ?: return
        val httpRepository = remoteRepository ?: return
        val version = sessionVersion
        viewModelScope.launch {
            runCatching {
                credential.remoteId?.let { httpRepository.deleteCredential(it) }
                localRepository.deleteCredential(credential.id)
                if (version != sessionVersion) return@launch
                uiState = uiState.copy(
                    credentials = localRepository.getCredentials(),
                    selectedCredentialId = null,
                    showDeleteDialog = false,
                    destination = VaultDestination.VAULT,
                    message = "Credencial eliminada"
                )
            }.onFailure { error ->
                if (version == sessionVersion) {
                    uiState = uiState.copy(
                        showDeleteDialog = false,
                        message = error.userMessage("No se pudo eliminar; no se realizaron cambios")
                    )
                }
            }
        }
    }

    private suspend fun syncFromRemote(version: Long, onFailure: (String) -> Unit) {
        if (uiState.isSyncing) return
        val localRepository = repository ?: return
        val httpRepository = remoteRepository ?: return
        uiState = uiState.copy(isSyncing = true)
        runCatching {
            val remoteCredentials = httpRepository.getCredentials()
            if (version != sessionVersion) return
            val localCredentials = localRepository.getCredentials()

            remoteCredentials.forEach { remote ->
                val local = localCredentials.firstOrNull { it.remoteId == remote.remoteId }
                localRepository.saveCredential(remote.copy(id = local?.id ?: 0L))
            }

            localCredentials.filter { it.remoteId == null }.forEach { pending ->
                val synchronized = httpRepository.saveCredential(pending)
                localRepository.saveCredential(synchronized)
            }

            if (version == sessionVersion) {
                uiState = uiState.copy(
                    credentials = localRepository.getCredentials(),
                    isSyncing = false,
                    message = "Bóveda sincronizada"
                )
            }
        }.onFailure { error ->
            if (version != sessionVersion) return
            if (error.isCryptographicFailure()) {
                failUnlock(version, error, onFailure)
            } else {
                uiState = uiState.copy(
                    credentials = runCatching { localRepository.getCredentials() }
                        .getOrDefault(uiState.credentials),
                    isSyncing = false,
                    message = "Sin conexión con la API; se muestran los datos locales"
                )
            }
        }
    }

    fun clearSession() {
        sessionVersion++
        crypto?.close()
        crypto = null
        repository = null
        remoteRepository = null
        uiState = VaultUiState(darkTheme = uiState.darkTheme)
    }

    fun setDarkTheme(enabled: Boolean) {
        uiState = uiState.copy(darkTheme = enabled)
    }

    fun setGeneratorLength(length: Int) {
        uiState = uiState.copy(generatorLength = length)
        regeneratePassword()
    }

    fun setGeneratorOptions(
        uppercase: Boolean = uiState.includeUppercase,
        lowercase: Boolean = uiState.includeLowercase,
        numbers: Boolean = uiState.includeNumbers,
        symbols: Boolean = uiState.includeSymbols
    ) {
        if (!uppercase && !lowercase && !numbers && !symbols) return
        uiState = uiState.copy(
            includeUppercase = uppercase,
            includeLowercase = lowercase,
            includeNumbers = numbers,
            includeSymbols = symbols
        )
        regeneratePassword()
    }

    fun regeneratePassword() {
        val alphabet = buildString {
            if (uiState.includeUppercase) append("ABCDEFGHJKLMNPQRSTUVWXYZ")
            if (uiState.includeLowercase) append("abcdefghijkmnopqrstuvwxyz")
            if (uiState.includeNumbers) append("23456789")
            if (uiState.includeSymbols) append("!@#$%&*")
        }
        if (alphabet.isEmpty()) return
        val password = buildString {
            repeat(uiState.generatorLength) { append(alphabet.random()) }
        }
        uiState = uiState.copy(generatedPassword = password)
    }

    fun useGeneratedPassword() {
        uiState = uiState.copy(
            draft = CredentialDraft(password = uiState.generatedPassword),
            destination = VaultDestination.EDITOR,
            message = "Contraseña agregada al formulario"
        )
    }

    fun showMessage(message: String) {
        uiState = uiState.copy(message = message)
    }

    fun clearMessage() {
        uiState = uiState.copy(message = null)
    }

    private suspend fun synchronizeCredential(credential: Credential, version: Long) {
        val localRepository = repository ?: return
        val httpRepository = remoteRepository ?: return
        runCatching { httpRepository.saveCredential(credential) }
            .onSuccess { synchronized ->
                if (version != sessionVersion) return
                localRepository.saveCredential(synchronized)
                uiState = uiState.copy(
                    credentials = localRepository.getCredentials(),
                    selectedCredentialId = synchronized.id,
                    message = "Cambios sincronizados"
                )
            }
            .onFailure {
                if (version == sessionVersion) {
                    uiState = uiState.copy(
                        message = "Guardada localmente; no se pudo sincronizar con la API"
                    )
                }
            }
    }

    private fun failUnlock(version: Long, error: Throwable, onFailure: (String) -> Unit) {
        if (version != sessionVersion) return
        clearSession()
        onFailure(
            if (error.isCryptographicFailure()) {
                "No se pudo descifrar la bóveda. Verifica la contraseña maestra. " +
                    "Si usabas la versión anterior, migra primero desde el dispositivo original."
            } else {
                error.userMessage("No se pudo abrir la bóveda local")
            }
        )
    }

    private fun currentOwnerId(): String = if (BuildConfig.FIREBASE_CONFIGURED) {
        FirebaseAuth.getInstance().currentUser?.uid ?: ANONYMOUS_OWNER
    } else {
        DEVELOPMENT_OWNER
    }

    private fun Throwable.isCryptographicFailure(): Boolean =
        generateSequence(this) { it.cause }.any { it is GeneralSecurityException }

    private fun Throwable.userMessage(fallback: String): String =
        localizedMessage?.takeIf { it.isNotBlank() } ?: fallback

    override fun onCleared() {
        crypto?.close()
        super.onCleared()
    }

    private companion object {
        const val DEVELOPMENT_OWNER = "demo"
        const val ANONYMOUS_OWNER = "anonymous"
    }
}
