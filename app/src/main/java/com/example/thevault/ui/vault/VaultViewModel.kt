package com.example.thevault.ui.vault

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.thevault.data.model.Credential
import com.example.thevault.data.model.CredentialCategory
import com.example.thevault.data.repository.VaultRepository
import com.example.thevault.data.repository.RoomVaultRepository

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
    val message: String? = null
)

class VaultViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: VaultRepository =
        RoomVaultRepository(application)

    var uiState by mutableStateOf(
        VaultUiState(credentials = repository.getCredentials())
    )
        private set

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

        val credential = Credential(
            id = draft.id ?: ((uiState.credentials.maxOfOrNull { it.id } ?: 0L) + 1L),
            name = draft.name.trim(),
            username = draft.username.trim(),
            password = draft.password,
            website = draft.website.trim(),
            notes = draft.notes.trim(),
            category = draft.category,
            isFavorite = draft.isFavorite
        )
        repository.saveCredential(credential)
        uiState = uiState.copy(
            credentials = repository.getCredentials(),
            selectedCredentialId = credential.id,
            destination = VaultDestination.DETAIL,
            message = if (draft.id == null) "Credencial agregada" else "Cambios guardados"
        )
    }

    fun toggleFavorite() {
        val credential = selectedCredential ?: return
        repository.saveCredential(credential.copy(isFavorite = !credential.isFavorite))
        uiState = uiState.copy(credentials = repository.getCredentials())
    }

    fun requestDelete() {
        uiState = uiState.copy(showDeleteDialog = true)
    }

    fun dismissDelete() {
        uiState = uiState.copy(showDeleteDialog = false)
    }

    fun deleteSelected() {
        val id = uiState.selectedCredentialId ?: return
        repository.deleteCredential(id)
        uiState = uiState.copy(
            credentials = repository.getCredentials(),
            selectedCredentialId = null,
            showDeleteDialog = false,
            destination = VaultDestination.VAULT,
            message = "Credencial eliminada"
        )
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
}
