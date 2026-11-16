package com.example.thevault

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.thevault.data.TheVaultDatabase
import com.example.thevault.data.auth.LocalAccessService
import com.example.thevault.data.auth.LocalAccountStore
import com.example.thevault.data.repository.RoomVaultRepository
import com.example.thevault.ui.auth.AuthStage
import com.example.thevault.ui.auth.AuthViewModel
import com.example.thevault.ui.vault.VaultViewModel
import java.io.File
import java.security.KeyStore
import java.util.UUID
import kotlinx.coroutines.cancel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RegistroLocalUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun registroConfirmacionIngresoYRecuperacionSinApi() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val id = UUID.randomUUID().toString()
        val directory = File(app.cacheDir, "registro-ui-$id")
        val alias = "thevault_ui_$id"
        val database = Room.inMemoryDatabaseBuilder(app, TheVaultDatabase::class.java).build()
        val accounts = LocalAccountStore(app, directory, alias)
        lateinit var auth: AuthViewModel
        lateinit var vault: VaultViewModel
        compose.runOnUiThread {
            auth = AuthViewModel(app, LocalAccessService(app, accounts, database))
            vault = VaultViewModel(app, { context, owner, cipher ->
                RoomVaultRepository(context, owner, cipher, database.credentialDao())
            }, { error("El acceso local no debe llamar a la API") })
        }
        try {
            val mounted = mutableStateOf(true)
            compose.setContent { if (mounted.value) TheVaultApp(auth, vault) }
            waitFor { !auth.uiState.showSplash }
            compose.onNodeWithText("Crea tu cuenta").assertIsDisplayed()
            compose.onNodeWithText("Correo electrónico").performTextInput("ui@ejemplo.com")
            compose.onNodeWithText("Contraseña maestra").performTextInput("Mi frase privada 2026")
            compose.onNodeWithText("Confirma la contraseña").performScrollTo().performTextInput("Mi frase privada 2026")
            compose.onNodeWithText("Ver mi clave de recuperación").performScrollTo().performClick()
            waitFor { auth.uiState.stage == AuthStage.RECOVERY_KEY && !auth.uiState.isLoading }
            val code = auth.uiState.recoveryCode
            assertFalse(accounts.hasAccounts())
            compose.onNodeWithText("Guardar cuenta y entrar").performScrollTo().performClick()
            assertFalse(accounts.hasAccounts())
            compose.onNode(isToggleable()).performScrollTo().performClick()
            compose.onNodeWithText("Últimos 8 caracteres").performScrollTo().performTextInput(code.takeLast(8))
            compose.onNodeWithText("Guardar cuenta y entrar").performScrollTo().performClick()
            waitFor { vault.uiState.isUnlocked }
            assertTrue(accounts.hasAccounts())
            compose.onNodeWithText("Mi bóveda").assertIsDisplayed()
            // Recrear la pantalla conserva el acceso de los modelos, sin pedir otra clave.
            compose.runOnUiThread { mounted.value = false }
            compose.waitForIdle()
            compose.runOnUiThread { mounted.value = true }
            compose.waitForIdle()
            assertTrue(auth.uiState.isAuthenticated)
            compose.onNodeWithText("Mi bóveda").assertIsDisplayed()
            compose.runOnUiThread {
                vault.startCreate()
                vault.updateDraft { it.copy(name = "Cuenta UI", username = "persona", password = "conservar") }
                vault.saveDraft()
            }
            waitFor { vault.uiState.credentials.size == 1 && !vault.uiState.isSaving }
            compose.runOnUiThread { vault.clearSession(); auth.logout() }
            compose.onNodeWithText("Contraseña maestra").performTextInput("Mi frase privada 2026")
            compose.onNodeWithText("Iniciar sesión").performScrollTo().performClick()
            waitFor { vault.uiState.isUnlocked }
            assertEquals(1, vault.uiState.credentials.size)
            compose.runOnUiThread { vault.clearSession(); auth.logout() }
            compose.onNodeWithText("Olvidé mi contraseña").performScrollTo().performClick()
            compose.onNodeWithText("Clave de recuperación completa").performScrollTo().performTextInput(code)
            compose.onNodeWithText("Nueva contraseña maestra").performScrollTo().performTextInput("Otra frase privada 2026")
            compose.onNodeWithText("Confirma la contraseña").performScrollTo().performTextInput("Otra frase privada 2026")
            compose.onNodeWithText("Cambiar contraseña y entrar").performScrollTo().performClick()
            waitFor { vault.uiState.isUnlocked }
            assertEquals("conservar", vault.uiState.credentials.single().password)
            compose.runOnUiThread { vault.openCredential(vault.uiState.credentials.single().id); vault.deleteSelected() }
            waitFor { vault.uiState.credentials.isEmpty() }
        } finally {
            compose.runOnUiThread { vault.clearSession(); vault.viewModelScope.cancel(); auth.logout(); auth.viewModelScope.cancel() }
            database.close(); directory.deleteRecursively()
            KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(alias) }
        }
    }

    private fun waitFor(condition: () -> Boolean) = compose.waitUntil(timeoutMillis = 120_000, condition = condition)
}
