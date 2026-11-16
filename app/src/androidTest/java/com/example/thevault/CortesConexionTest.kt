package com.example.thevault

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.thevault.data.auth.ApiTokenProvider
import com.example.thevault.data.TheVaultDatabase
import com.example.thevault.data.remote.HttpRemoteVaultRepository
import com.example.thevault.data.repository.VaultRepository
import com.example.thevault.data.repository.RoomVaultRepository
import com.example.thevault.ui.vault.VaultViewModel
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.Before
import org.junit.Assume.assumeTrue

// Requiere scripts/probar-cortes-api.py y el argumento api_cortes=true.
class CortesConexionTest {
    @Before fun requiereApiDePrueba() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("api_cortes") == "true")
    }

    @Test
    fun guardarSinConexionYReconectarMantieneUnaCuenta() = runBlocking<Unit> {
        val prueba = Prueba()
        try {
            prueba.abrir()
            control("/desconectar")
            principal { prueba.crearYGuardar() }
            esperar { !prueba.vm.uiState.isSaving && prueba.vm.uiState.credentials.size == 1 }
            assertEquals(0, control("/estado").getInt("cuentas"))
            control("/conectar")
            prueba.reabrir()
            assertEquals(1, control("/estado").getInt("cuentas"))
            assertEquals(1, prueba.local.getCredentials().size)
        } finally { prueba.cerrar() }
    }

    @Test
    fun cortarAntesDeQueLaApiGuardeMantieneUnaCuentaAlReconectar() = runBlocking<Unit> {
        val prueba = Prueba()
        try {
            prueba.abrir()
            control("/modo?valor=antes")
            principal { prueba.crearYGuardar() }
            esperar { control("/estado").getBoolean("antes") }
            control("/desconectar")
            esperar { !prueba.vm.uiState.isSaving }
            assertEquals(0, control("/estado").getInt("cuentas"))
            control("/conectar")
            prueba.reabrir()
            assertEquals(1, control("/estado").getInt("cuentas"))
            assertEquals(1, prueba.local.getCredentials().size)
        } finally { prueba.cerrar() }
    }

    @Test
    fun perderLaRespuestaDespuesDeCrearNoDebeDuplicarAlReconectar() = runBlocking<Unit> {
        val prueba = Prueba()
        try {
            prueba.abrir()
            control("/modo?valor=despues")
            principal { prueba.crearYGuardar() }
            esperar { control("/estado").getBoolean("despues") }
            assertEquals(1, control("/estado").getInt("cuentas"))
            control("/desconectar")
            esperar { !prueba.vm.uiState.isSaving }
            assertEquals(null, prueba.local.getCredentials().single().remoteId)
            control("/conectar")
            prueba.reabrir()
            val servidor = control("/estado").getInt("cuentas")
            val telefono = prueba.local.getCredentials().size
            assertEquals("Tras perder la respuesta: servidor=$servidor, teléfono=$telefono", 1, servidor)
            assertEquals(1, telefono)
        } finally { prueba.cerrar() }
    }

    private class Prueba {
        private val database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Application>(), TheVaultDatabase::class.java
        ).build()
        lateinit var local: VaultRepository
        val fallo = AtomicReference<String?>()
        val vm: VaultViewModel

        init {
            control("/reiniciar")
            var modelo: VaultViewModel? = null
            principal {
                modelo = VaultViewModel(
                    ApplicationProvider.getApplicationContext<Application>(),
                    { app, _, cipher -> RoomVaultRepository(app, { "prueba-cortes" }, cipher, database.credentialDao())
                        .also { local = it } },
                    { cipher -> HttpRemoteVaultRepository(
                        "http://localhost:18082",
                        object : ApiTokenProvider { override suspend fun idToken() = "dev:prueba-cortes" },
                        cipher
                    ) }
                )
            }
            vm = checkNotNull(modelo)
        }

        suspend fun abrir() {
            principal { vm.unlock("solo-pruebas-de-cortes".toCharArray()) { fallo.set(it) } }
            esperar {
                check(fallo.get() == null) { fallo.get().orEmpty() }
                vm.uiState.isUnlocked && !vm.uiState.isSyncing
            }
        }

        suspend fun reabrir() {
            principal { vm.clearSession() }
            abrir()
        }

        fun crearYGuardar() {
            vm.startCreate()
            vm.updateDraft { it.copy(name = "Cuenta de cortes", username = "usuario-prueba", password = "ejemplo") }
            vm.saveDraft()
        }

        fun cerrar() {
            principal { vm.clearSession(); vm.viewModelScope.cancel() }
            try { control("/desconectar") } finally { database.close() }
        }
    }

    private companion object {
        fun principal(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
        suspend fun esperar(condition: () -> Boolean) {
            withTimeout(90_000) { while (!condition()) delay(30) }
        }
        fun control(path: String): JSONObject {
            val connection = URL("http://localhost:18083$path").openConnection() as HttpURLConnection
            connection.connectTimeout = 3_000
            connection.readTimeout = 3_000
            return try {
                connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
            } finally { connection.disconnect() }
        }
    }
}
