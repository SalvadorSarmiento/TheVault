package com.example.thevault

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.thevault.data.model.Credential
import com.example.thevault.data.remote.RemoteVaultRepository
import com.example.thevault.data.repository.VaultRepository
import com.example.thevault.ui.vault.VaultViewModel
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CredencialesDuplicadasTest {
    @Test
    fun unGuardadoDuranteLaDescargaNoCreaDosCuentas() = runBlocking<Unit> {
        val prueba = Prueba(descargaLenta = true)
        try {
            esperar { prueba.vm.uiState.isUnlocked && prueba.remoto.descargaIniciada.isCompleted }
            principal { prueba.nuevaCuenta(); prueba.vm.saveDraft() }
            prueba.remoto.permitirDescarga.complete(Unit)
            esperar { prueba.remoto.solicitudes.isNotEmpty() }
            principal { }
            prueba.remoto.permitirGuardado.complete(Unit)
            esperar { !prueba.vm.uiState.isSyncing && prueba.remoto.activas.get() == 0 }
            principal { }
            assertEquals("Un toque debe crear una sola cuenta remota", 1, prueba.remoto.creaciones())
            assertEquals(1, prueba.local.getCredentials().size)
        } finally { prueba.cerrar() }
    }

    @Test
    fun editarAntesDeRecibirLaRespuestaNoCreaOtraCuenta() = runBlocking<Unit> {
        val prueba = Prueba()
        try {
            esperar { prueba.vm.uiState.isUnlocked && !prueba.vm.uiState.isSyncing }
            principal { prueba.nuevaCuenta(); prueba.vm.saveDraft() }
            esperar { prueba.remoto.solicitudes.size == 1 }
            principal {
                prueba.vm.startEdit()
                prueba.vm.updateDraft { it.copy(name = "Cuenta editada") }
                prueba.vm.saveDraft()
            }
            prueba.remoto.permitirGuardado.complete(Unit)
            principal { }
            esperar { prueba.remoto.activas.get() == 0 }
            principal { prueba.vm.saveDraft() }
            principal { }
            assertEquals("Editar debe actualizar el mismo registro remoto", 1, prueba.remoto.creaciones())
            assertEquals("Cuenta editada", prueba.local.getCredentials().single().name)
            assertEquals(1, prueba.remoto.cuentas().size)
        } finally { prueba.cerrar() }
    }

    @Test
    fun dosToquesRapidosNoInsertanDosFilas() = runBlocking<Unit> {
        val prueba = Prueba(guardadoLocalLento = true)
        try {
            esperar { prueba.vm.uiState.isUnlocked && !prueba.vm.uiState.isSyncing }
            prueba.remoto.permitirGuardado.complete(Unit)
            principal {
                prueba.nuevaCuenta()
                prueba.vm.saveDraft()
                prueba.vm.saveDraft()
            }
            prueba.local.permitirGuardado.complete(Unit)
            esperar { prueba.remoto.solicitudes.isNotEmpty() }
            principal { }
            assertEquals("Los toques repetidos deben guardar una sola vez", 1, prueba.local.getCredentials().size)
        } finally { prueba.cerrar() }
    }

    @Test
    fun unFalloDeConexionPermiteReintentarSinDuplicarLaCuentaLocal() = runBlocking<Unit> {
        val prueba = Prueba()
        try {
            esperar { prueba.vm.uiState.isUnlocked && !prueba.vm.uiState.isSyncing }
            prueba.remoto.fallarUnaVez = true
            prueba.remoto.permitirGuardado.complete(Unit)
            principal { prueba.nuevaCuenta(); prueba.vm.saveDraft() }
            esperar { prueba.vm.uiState.credentials.size == 1 && !prueba.vm.uiState.isSaving }
            principal {
                prueba.vm.startEdit()
                prueba.vm.updateDraft { it.copy(name = "Cuenta recuperada") }
                prueba.vm.saveDraft()
            }
            principal { }
            assertEquals(1, prueba.local.getCredentials().size)
            assertEquals("Cuenta recuperada", prueba.local.getCredentials().single().name)
            assertEquals(1, prueba.remoto.cuentas().size)
            assertFalse(prueba.vm.uiState.isSaving)
        } finally { prueba.cerrar() }
    }

    private class Prueba(descargaLenta: Boolean = false, guardadoLocalLento: Boolean = false) {
        val local = Local(guardadoLocalLento)
        val remoto = Servidor(descargaLenta)
        val vm: VaultViewModel

        init {
            var modelo: VaultViewModel? = null
            principal {
                modelo = VaultViewModel(
                    ApplicationProvider.getApplicationContext<Application>(),
                    { _, _, _ -> local },
                    { _ -> remoto }
                ).also { it.unlock("solo-para-pruebas".toCharArray(), ownerId = "prueba-duplicados", syncEnabled = true) { error(it) } }
            }
            vm = checkNotNull(modelo)
        }

        fun nuevaCuenta() {
            vm.startCreate()
            vm.updateDraft { it.copy(name = "Cuenta de prueba", username = "prueba", password = "ejemplo") }
        }

        fun cerrar() {
            principal { vm.clearSession(); vm.viewModelScope.cancel() }
            local.permitirGuardado.complete(Unit)
            remoto.permitirDescarga.complete(Unit)
            remoto.permitirGuardado.complete(Unit)
        }
    }

    private class Local(lento: Boolean) : VaultRepository {
        val permitirGuardado = CompletableDeferred<Unit>().also { if (!lento) it.complete(Unit) }
        private val registros = linkedMapOf<Long, Credential>()
        private var siguienteId = 0L

        override suspend fun getCredentials(): List<Credential> = synchronized(registros) { registros.values.toList() }

        override suspend fun saveCredential(credential: Credential): Credential {
            permitirGuardado.await()
            return synchronized(registros) {
                val id = if (registros.containsKey(credential.id)) credential.id else ++siguienteId
                credential.copy(id = id).also { registros[id] = it }
            }
        }

        override suspend fun deleteCredential(id: Long) { synchronized(registros) { registros.remove(id) } }
    }

    private class Servidor(lento: Boolean) : RemoteVaultRepository {
        val permitirDescarga = CompletableDeferred<Unit>().also { if (!lento) it.complete(Unit) }
        val descargaIniciada = CompletableDeferred<Unit>()
        val permitirGuardado = CompletableDeferred<Unit>()
        val solicitudes = CopyOnWriteArrayList<Credential>()
        val activas = AtomicInteger()
        @Volatile var fallarUnaVez = false
        private val registros = linkedMapOf<String, Credential>()
        private var siguienteId = 0

        override suspend fun getCredentials(): List<Credential> {
            descargaIniciada.complete(Unit)
            permitirDescarga.await()
            return cuentas()
        }

        override suspend fun saveCredential(credential: Credential): Credential {
            solicitudes.add(credential)
            activas.incrementAndGet()
            try {
                permitirGuardado.await()
                if (fallarUnaVez) {
                    fallarUnaVez = false
                    throw IOException("Fallo de conexión de prueba, antes de crear la cuenta")
                }
                return synchronized(registros) {
                    val id = credential.remoteId ?: "remoto-${++siguienteId}"
                    credential.copy(remoteId = id).also { registros[id] = it }
                }
            } finally { activas.decrementAndGet() }
        }

        fun cuentas(): List<Credential> = synchronized(registros) { registros.values.toList() }
        fun creaciones(): Int = solicitudes.count { it.remoteId == null }
        override suspend fun deleteCredential(remoteId: String) { synchronized(registros) { registros.remove(remoteId) } }
    }

    private companion object {
        fun principal(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
        suspend fun esperar(condition: () -> Boolean) {
            withTimeout(10_000) { while (!condition()) delay(10) }
        }
    }
}
