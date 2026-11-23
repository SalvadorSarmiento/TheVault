package com.example.thevault

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso
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
            compose.onNodeWithText("Conocer TheVault").assertIsDisplayed().performClick()
            pulse("Continuar")
            compose.onNodeWithText("Crear mi cuenta").performClick()
            compose.onNodeWithText("Contraseña maestra").assertDoesNotExist()
            compose.onNodeWithText("Correo electrónico").performTextInput("correo-incompleto")
            pulse("Continuar")
            assertEquals(AuthStage.REGISTER, auth.uiState.stage)
            compose.onNodeWithText("Correo electrónico").performTextClearance()
            compose.onNodeWithText("Correo electrónico").performTextInput("ui@ejemplo.com")
            pulse("Continuar")
            compose.runOnIdle { assertEquals(auth.uiState.error, AuthStage.REGISTER_PASSWORD, auth.uiState.stage) }
            compose.onNodeWithText("Correo electrónico").assertDoesNotExist()
            compose.onNodeWithContentDescription("Volver al paso anterior").performClick()
            compose.onNodeWithText("ui@ejemplo.com").assertIsDisplayed()
            pulse("Continuar")
            compose.onNodeWithText("Contraseña maestra").performTextInput("corta")
            compose.onNodeWithText("Confirma la contraseña").performTextInput("corta")
            pulse("Continuar")
            assertEquals(AuthStage.REGISTER_PASSWORD, auth.uiState.stage)
            assertFalse(accounts.hasAccounts())
            compose.onNodeWithText("Contraseña maestra").performTextClearance()
            compose.onNodeWithText("Confirma la contraseña").performTextClearance()
            compose.onNodeWithText("Contraseña maestra").performTextInput("Mi frase privada 2026")
            compose.onNodeWithText("Confirma la contraseña").performScrollTo().performTextInput("Mi frase privada 2026")
            pulse("Continuar")
            waitFor { auth.uiState.stage == AuthStage.RECOVERY_KEY && !auth.uiState.isLoading }
            val code = auth.uiState.recoveryCode
            assertFalse(accounts.hasAccounts())
            compose.onNodeWithText("Últimos 8 caracteres").assertDoesNotExist()
            compose.onNodeWithText("Ya la anoté").performClick()
            pulse("Guardar cuenta y entrar")
            assertFalse(accounts.hasAccounts())
            compose.onNodeWithContentDescription("Volver al paso anterior").performClick()
            assertEquals(code, auth.uiState.recoveryCode)
            compose.onNodeWithText("Ya la anoté").performClick()
            compose.onNode(isToggleable()).performScrollTo().performClick()
            compose.onNodeWithText("Últimos 8 caracteres").performScrollTo().performTextInput(code.takeLast(8))
            pulse("Guardar cuenta y entrar")
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
            compose.onNodeWithContentDescription("Añadir credencial").performClick()
            compose.onNodeWithText("Guardar credencial").assertIsDisplayed()
            compose.onNodeWithText("Nombre").performTextInput("Cuenta UI")
            compose.onNodeWithText("Usuario o correo").performScrollTo().performTextInput("persona")
            compose.onNodeWithText("Contraseña").performScrollTo().performTextInput("conservar")
            pulse("Guardar credencial")
            waitFor { vault.uiState.credentials.size == 1 && !vault.uiState.isSaving }
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Editar credencial"))
            compose.onNodeWithText("Editar credencial").performClick()
            compose.onNodeWithText("Nombre").performTextReplacement("Cuenta UI editada")
            pulse("Guardar cambios")
            waitFor { !vault.uiState.isSaving && vault.uiState.credentials.single().name == "Cuenta UI editada" }
            assertEquals(1, vault.uiState.credentials.size)
            compose.runOnUiThread { vault.clearSession(); auth.logout() }
            assertEquals(AuthStage.LOGIN, auth.uiState.stage)
            compose.onNodeWithText("Contraseña maestra").performTextInput("Mi frase privada 2026")
            pulse("Iniciar sesión")
            waitFor { vault.uiState.isUnlocked }
            assertEquals(1, vault.uiState.credentials.size)
            compose.runOnUiThread { vault.clearSession(); auth.logout() }
            compose.onNodeWithText("Olvidé mi contraseña").performClick()
            compose.onNodeWithText("Nueva contraseña maestra").assertDoesNotExist()
            compose.onNodeWithText("Clave de recuperación completa").performScrollTo().performTextInput(code)
            pulse("Continuar")
            compose.onNodeWithText("Nueva contraseña maestra").performScrollTo().performTextInput("Otra frase privada 2026")
            compose.onNodeWithText("Confirma la contraseña").performScrollTo().performTextInput("Otra frase privada 2026")
            pulse("Cambiar contraseña y entrar")
            waitFor { vault.uiState.isUnlocked }
            assertEquals("conservar", vault.uiState.credentials.single().password)
            compose.runOnUiThread { vault.openCredential(vault.uiState.credentials.single().id) }
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasContentDescription("Mostrar contraseña"))
            compose.onNodeWithContentDescription("Mostrar contraseña").performClick()
            compose.onNodeWithText("conservar").assertIsDisplayed()
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Eliminar credencial"))
            compose.onNodeWithText("Eliminar credencial").performClick()
            compose.onNodeWithText("Cancelar").performClick()
            assertEquals(1, vault.uiState.credentials.size)
            compose.onNodeWithText("Eliminar credencial").performClick()
            compose.onNodeWithText("Eliminar", substring = false).performClick()
            waitFor { vault.uiState.credentials.isEmpty() }
            lateinit var returning: AuthViewModel
            compose.runOnUiThread { returning = AuthViewModel(app, LocalAccessService(app, accounts, database)) }
            try {
                waitFor { !returning.uiState.showSplash }
                assertEquals("Una cuenta existente no debe repetir la presentación", AuthStage.LOGIN, returning.uiState.stage)
            } finally { compose.runOnUiThread { returning.viewModelScope.cancel() } }
        } finally {
            compose.runOnUiThread { vault.clearSession(); vault.viewModelScope.cancel(); auth.logout(); auth.viewModelScope.cancel() }
            database.close(); directory.deleteRecursively()
            KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(alias) }
        }
    }

    private fun pulse(text: String) {
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        compose.onNodeWithText(text).performClick()
    }

    private fun waitFor(condition: () -> Boolean) = compose.waitUntil(timeoutMillis = 120_000, condition = condition)
}
