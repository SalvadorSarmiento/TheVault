package com.example.thevault

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import android.app.Application
import kotlinx.coroutines.cancel
import org.junit.Assert.*
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.thevault.data.model.Credential
import com.example.thevault.data.model.CredentialCategory
import com.example.thevault.ui.auth.AuthUiState
import com.example.thevault.ui.auth.AuthStage
import com.example.thevault.ui.screens.LoginScreen
import com.example.thevault.ui.screens.SplashScreen
import com.example.thevault.ui.screens.VaultListScreen
import com.example.thevault.ui.screens.CredentialDetailScreen
import com.example.thevault.ui.screens.CredentialEditorScreen
import com.example.thevault.ui.screens.CategoriesScreen
import com.example.thevault.ui.screens.PasswordGeneratorScreen
import com.example.thevault.ui.screens.SettingsScreen
import com.example.thevault.ui.vault.CredentialDraft
import com.example.thevault.ui.vault.VaultViewModel
import com.example.thevault.ui.vault.VaultDestination
import com.example.thevault.ui.theme.TheVaultTheme
import com.example.thevault.ui.vault.VaultUiState
import java.io.File
import org.junit.Rule
import org.junit.Test

// Imágenes de revisión con cuentas ficticias. No abre la bóveda real.
class VistasDisenoTest {
    @get:Rule val vista = createComposeRule()

    @Test fun bienvenida() = guardar("bienvenida", false) { SplashScreen() }

    @Test fun inicio() = guardar("inicio", false) {
        LoginScreen(AuthUiState(showSplash = false), {}, {}, {}, {})
    }

    @Test fun inicioOscuro() = guardar("inicio-oscuro", true) {
        LoginScreen(AuthUiState(showSplash = false), {}, {}, {}, {})
    }

    @Test fun registroLocal() = guardar("registro-local", false) {
        LoginScreen(AuthUiState(showSplash = false, stage = AuthStage.REGISTER), {}, {}, {}, {})
    }

    @Test fun presentacion() = acceso("presentacion", AuthStage.WELCOME)
    @Test fun privacidad() = acceso("privacidad", AuthStage.PRIVACY)
    @Test fun explicaRecuperacion() = acceso("explica-recuperacion", AuthStage.KEY_EXPLANATION)
    @Test fun contrasena() = acceso("contrasena", AuthStage.REGISTER_PASSWORD)
    @Test fun confirmaClave() = acceso("confirma-clave", AuthStage.CONFIRM_KEY)
    @Test fun recuperarAcceso() = acceso("recuperar-acceso", AuthStage.RECOVER)

    private fun acceso(nombre: String, paso: AuthStage) = guardar(nombre, false) {
        LoginScreen(AuthUiState(showSplash = false, stage = paso), {}, {}, {}, {})
    }

    @Test fun confirmarEnPantallaPequenaConTextoGrande() {
        vista.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.3f)) {
                TheVaultTheme {
                    Box(Modifier.size(320.dp, 480.dp)) {
                        LoginScreen(AuthUiState(showSplash = false, stage = AuthStage.CONFIRM_KEY), {}, {}, {}, {})
                    }
                }
            }
        }
        vista.onNodeWithText("Guardar cuenta y entrar").assertIsDisplayed()
        vista.onNodeWithText("Últimos 8 caracteres").performScrollTo().assertIsDisplayed()
    }

    @Test fun claveRecuperacion() = guardar("clave-recuperacion", false) {
        // Clave ficticia solo para revisar el diseño.
        LoginScreen(AuthUiState(showSplash = false, stage = AuthStage.RECOVERY_KEY,
            recoveryCode = List(8) { "A1B2C3D4" }.joinToString("-")), {}, {}, {}, {})
    }

    @Test fun bovedaClara() = boveda("boveda-clara", false)
    @Test fun bovedaOscura() = boveda("boveda-oscura", true)

    @Test fun detalle() = guardar("detalle-cuenta", false) {
        CredentialDetailScreen(cuentaDeMuestra(), false, {}, {}, {}, {}, {}, {}, {})
    }

    @Test fun detalleOscuro() = guardar("detalle-oscuro", true) {
        CredentialDetailScreen(cuentaDeMuestra(), false, {}, {}, {}, {}, {}, {}, {})
    }

    @Test fun editor() = guardar("editor-cuenta", false) {
        CredentialEditorScreen(CredentialDraft(name = "Mi cuenta personal", username = "persona@ejemplo.com"), {}, {}, {})
    }

    @Test fun categorias() = guardar("categorias-cuentas", false) {
        CategoriesScreen(listOf(cuentaDeMuestra()), {})
    }

    @Test fun ajustes() = guardar("ajustes-cuentas", false) { SettingsScreen(false, {}, {}) }

    @Test fun generador() {
        val vm = nuevoGenerador()
        try {
            guardar("generador-cuentas", false) {
                PasswordGeneratorScreen(VaultUiState(generatedPassword = "Ab3!Cd4@Ef5#Gh6!Ij7@Kl8#Mn9!Op0@", generatorLength = 32), vm)
            }
        } finally { vista.runOnUiThread { vm.viewModelScope.cancel() } }
    }

    @Test fun editorPequenoConTextoGrande() {
        vista.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.3f)) {
                TheVaultTheme {
                    Box(Modifier.size(320.dp, 480.dp)) {
                        CredentialEditorScreen(CredentialDraft(), {}, {}, {})
                    }
                }
            }
        }
        vista.onNodeWithText("Guardar credencial").assertIsDisplayed()
        vista.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Notas (opcional)"))
        vista.onNodeWithText("Notas (opcional)").assertIsDisplayed()
        vista.onNodeWithText("Guardar credencial").assertIsDisplayed()
    }

    @Test fun opcionesDelGeneradorSiguenFuncionando() {
        val vm = nuevoGenerador()
        try {
            vista.setContent { TheVaultTheme { PasswordGeneratorScreen(vm.uiState, vm) } }
            listOf("Mayúsculas (A–Z)", "Minúsculas (a–z)", "Números (0–9)", "Símbolos (!@#$)").forEach { label ->
                vista.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(label))
                vista.onNodeWithText(label).performClick()
            }
            vista.runOnIdle {
                assertFalse(vm.uiState.includeUppercase)
                assertFalse(vm.uiState.includeLowercase)
                assertFalse(vm.uiState.includeNumbers)
                assertTrue(vm.uiState.includeSymbols)
            }
            vista.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Usar contraseña"))
            vista.onNodeWithText("Usar contraseña").performClick()
            vista.runOnIdle {
                assertEquals(VaultDestination.EDITOR, vm.uiState.destination)
                assertEquals(vm.uiState.generatedPassword, vm.uiState.draft.password)
            }
        } finally { vista.runOnUiThread { vm.viewModelScope.cancel() } }
    }

    private fun nuevoGenerador(): VaultViewModel {
        lateinit var vm: VaultViewModel
        vista.runOnUiThread { vm = VaultViewModel(ApplicationProvider.getApplicationContext<Application>()) }
        return vm
    }

    private fun cuentaDeMuestra() = Credential(id = 1, name = "Mi cuenta personal", username = "persona@ejemplo.com",
        password = "solo-una-muestra", website = "https://ejemplo.com", notes = "Datos ficticios para revisar el diseño.")

    private fun boveda(nombre: String, oscuro: Boolean) {
        val cuentas = listOf(
            Credential(id = 1, name = "Correo personal", username = "ana@ejemplo.com", password = "ejemplo", isFavorite = true),
            Credential(id = 2, name = "Mi trabajo", username = "ana.trabajo", password = "ejemplo", category = CredentialCategory.WORK),
            Credential(id = 3, name = "Universidad", username = "ana.estudios", password = "ejemplo", category = CredentialCategory.STUDIES),
            Credential(id = 4, name = "Mis compras", username = "ana.compras", password = "ejemplo", category = CredentialCategory.SHOPPING)
        )
        guardar(nombre, oscuro) {
            VaultListScreen(VaultUiState(credentials = cuentas), cuentas, {}, {}, {})
        }
    }

    private fun guardar(nombre: String, oscuro: Boolean, contenido: @Composable () -> Unit) {
        vista.setContent {
            TheVaultTheme(darkTheme = oscuro) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { contenido() }
            }
        }
        vista.waitForIdle()
        val carpeta = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "vistas-diseno")
        check(carpeta.mkdirs() || carpeta.isDirectory)
        File(carpeta, "$nombre.png").outputStream().use {
            vista.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
