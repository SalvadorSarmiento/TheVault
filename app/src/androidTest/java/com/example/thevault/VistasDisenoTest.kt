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
import androidx.test.platform.app.InstrumentationRegistry
import com.example.thevault.data.model.Credential
import com.example.thevault.data.model.CredentialCategory
import com.example.thevault.ui.auth.AuthUiState
import com.example.thevault.ui.screens.LoginScreen
import com.example.thevault.ui.screens.VaultListScreen
import com.example.thevault.ui.theme.TheVaultTheme
import com.example.thevault.ui.vault.VaultUiState
import java.io.File
import org.junit.Rule
import org.junit.Test

// Imágenes de revisión con cuentas ficticias. No abre la bóveda real.
class VistasDisenoTest {
    @get:Rule val vista = createComposeRule()

    @Test fun inicio() = guardar("inicio", false) {
        LoginScreen(AuthUiState(showSplash = false), {}, {}, {}, {})
    }

    @Test fun inicioOscuro() = guardar("inicio-oscuro", true) {
        LoginScreen(AuthUiState(showSplash = false), {}, {}, {}, {})
    }

    @Test fun bovedaClara() = boveda("boveda-clara", false)
    @Test fun bovedaOscura() = boveda("boveda-oscura", true)

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
