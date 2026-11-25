package com.example.thevault

import android.app.Application
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.example.thevault.data.transfer.BluetoothPeer
import com.example.thevault.data.transfer.VaultBluetoothTransport
import com.example.thevault.ui.screens.BluetoothTransferScreen
import com.example.thevault.ui.theme.TheVaultTheme
import com.example.thevault.ui.transfer.BluetoothTransferViewModel
import com.example.thevault.ui.transfer.TransferStep
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class BluetoothTransferUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun separaEleccionRecepcionYRestauracion() {
        val transport = FakeTransport()
        val vm = model(transport)
        try {
            compose.onNodeWithText("Elige el otro teléfono.").assertIsDisplayed()
            capture("bluetooth-elegir")
            compose.onNodeWithText("Teléfono de prueba").performScrollTo().performClick()
            compose.onNodeWithText("Continuar").performClick()
            compose.onNodeWithText("Todo listo para recibir.").assertIsDisplayed()
            compose.onNodeWithText("Esperar copia").performClick()
            compose.waitUntil(10_000) { vm.uiState.step == TransferStep.WAITING }
            transport.received.complete(byteArrayOf(1, 2, 3))
            compose.waitUntil(10_000) { vm.uiState.step == TransferStep.RESTORE }
            compose.onNodeWithText("Recupera tu bóveda.").assertIsDisplayed()
            compose.onNodeWithText("Cerrar").assertIsDisplayed()
            compose.onNodeWithText("Clave de recuperación completa").assertExists()
            compose.onNodeWithText("Restaurar cuenta").assertIsDisplayed()
            capture("bluetooth-restaurar")
            assertFalse(vm.uiState.sending)
        } finally { compose.runOnIdle { vm.close(); vm.viewModelScope.cancel() } }
    }

    @Test fun permiteCancelarLaEsperaYExplicaElPermisoDenegado() {
        val transport = FakeTransport()
        val vm = model(transport)
        try {
            compose.runOnIdle { vm.permissionDenied() }
            compose.onNodeWithText("Sin el permiso de dispositivos cercanos", substring = true).assertExists()
            compose.onNodeWithText("Teléfono de prueba").performScrollTo().performClick()
            compose.onNodeWithText("Continuar").performClick()
            compose.onNodeWithText("Esperar copia").performClick()
            compose.waitUntil(10_000) { vm.uiState.busy }
            val closedBefore = transport.closeCount
            compose.onNodeWithText("Cancelar").performClick()
            compose.runOnIdle {
                assertFalse(vm.uiState.open)
                assertEquals(closedBefore + 1, transport.closeCount)
                assertEquals("", vm.uiState.recovery)
            }
        } finally { compose.runOnIdle { vm.close(); vm.viewModelScope.cancel() } }
    }

    private fun model(transport: FakeTransport): BluetoothTransferViewModel {
        lateinit var vm: BluetoothTransferViewModel
        compose.runOnUiThread {
            vm = BluetoothTransferViewModel(ApplicationProvider.getApplicationContext<Application>(), transport)
            vm.openReceiver(); vm.refresh()
        }
        compose.setContent { TheVaultTheme(darkTheme = true) { BluetoothTransferScreen(vm) {} } }
        return vm
    }

    private fun capture(name: String) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val folder = File(app.getExternalFilesDir(null), "bluetooth-preview").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private class FakeTransport : VaultBluetoothTransport {
        val received = CompletableDeferred<ByteArray>()
        var closeCount = 0
        override fun pairedDevices() = listOf(BluetoothPeer("00:11:22:33:44:55", "Teléfono de prueba"))
        override suspend fun receive(peer: BluetoothPeer) = received.await()
        override suspend fun send(peer: BluetoothPeer, packet: ByteArray) = error("No debe enviar en estas pruebas")
        override fun close() { closeCount++ }
    }
}
