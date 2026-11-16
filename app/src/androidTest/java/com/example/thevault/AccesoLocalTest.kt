package com.example.thevault

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.thevault.data.TheVaultDatabase
import com.example.thevault.data.auth.LocalAccessService
import com.example.thevault.data.auth.LocalAccountStore
import com.example.thevault.data.model.Credential
import com.example.thevault.data.repository.RoomVaultRepository
import com.example.thevault.data.security.MigratingCredentialCipher
import java.io.File
import java.security.KeyStore
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AccesoLocalTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val id = UUID.randomUUID().toString()
    private val directory = File(context.cacheDir, "acceso-test-$id")
    private val alias = "thevault_test_$id"
    private lateinit var database: TheVaultDatabase
    private lateinit var accounts: LocalAccountStore
    private lateinit var service: LocalAccessService
    private val password = "Frase de prueba 2026".toCharArray()

    @Before fun prepare() {
        database = Room.inMemoryDatabaseBuilder(context, TheVaultDatabase::class.java).build()
        accounts = LocalAccountStore(context, directory, alias)
        service = LocalAccessService(context, accounts, database)
    }

    @After fun clean() {
        database.close()
        directory.deleteRecursively()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(alias) }
        password.fill('\u0000')
    }

    @Test fun noSeGuardaAntesDeConfirmarYElArchivoEstaCifrado() = runBlocking<Unit> {
        val pending = accounts.prepare(" persona@ejemplo.com ", password)
        assertFalse(accounts.hasAccounts())
        assertFalse(directory.exists())
        val code = pending.recoveryCode
        service.complete(pending, null).close()
        assertTrue(accounts.hasAccounts())
        val saved = directory.listFiles()!!.single().readText()
        assertFalse(saved.contains("persona@ejemplo.com"))
        assertFalse(saved.contains(String(password)))
        assertFalse(saved.contains(code))
        assertFalse(saved.contains(code.replace("-", "")))
        accounts.login("PERSONA@ejemplo.com", password).use { assertEquals(pending.id, it.accountId) }
        fails { accounts.login("persona@ejemplo.com", "incorrecta".toCharArray()) }
        fails { accounts.prepare("persona@ejemplo.com", password) }
    }

    @Test fun recuperarCambiaLaContrasenaSinPerderCredenciales() = runBlocking<Unit> {
        val pending = accounts.prepare("recuperar@ejemplo.com", password)
        val code = pending.recoveryCode
        val session = service.complete(pending, null)
        MigratingCredentialCipher(context, session.vaultPassword).use { cipher ->
            RoomVaultRepository(context, { session.accountId }, cipher, database.credentialDao())
                .saveCredential(Credential(name = "Mi cuenta", username = "persona", password = "dato privado"))
        }
        val original = database.credentialDao().getAllCredentials(session.accountId).single()
        val diskBefore = directory.listFiles()!!.single().readBytes()
        fails { accounts.recover("recuperar@ejemplo.com", "0".repeat(64), "Una frase diferente".toCharArray()) }
        assertArrayEquals(diskBefore, directory.listFiles()!!.single().readBytes())
        val newPassword = "Una frase diferente".toCharArray()
        accounts.recover("recuperar@ejemplo.com", code.lowercase().replace("-", " "), newPassword).use { recovered ->
            assertArrayEquals(session.vaultPassword, recovered.vaultPassword)
            assertEquals(original, database.credentialDao().getAllCredentials(recovered.accountId).single())
            MigratingCredentialCipher(context, recovered.vaultPassword).use { cipher ->
                val rows = RoomVaultRepository(context, { recovered.accountId }, cipher, database.credentialDao()).getCredentials()
                assertEquals("dato privado", rows.single().password)
            }
        }
        fails { accounts.login("recuperar@ejemplo.com", password) }
        accounts.login("recuperar@ejemplo.com", newPassword).close()
        newPassword.fill('\u0000'); session.close()
    }

    @Test fun dosCuentasTienenClavesYBovedasDistintas() = runBlocking<Unit> {
        val first = service.complete(accounts.prepare("uno@ejemplo.com", password), null)
        val second = service.complete(accounts.prepare("dos@ejemplo.com", password), null)
        try {
            assertNotEquals(first.accountId, second.accountId)
            assertFalse(first.vaultPassword.contentEquals(second.vaultPassword))
            MigratingCredentialCipher(context, first.vaultPassword).use { cipher ->
                RoomVaultRepository(context, { first.accountId }, cipher, database.credentialDao())
                    .saveCredential(Credential(name = "Privada", username = "uno", password = "secreto"))
            }
            assertTrue(database.credentialDao().getAllCredentials(second.accountId).isEmpty())
        } finally { first.close(); second.close() }
    }

    @Test fun importarExigeLaContrasenaAnteriorYConservaLosDatos() = runBlocking<Unit> {
        val oldPassword = "clave anterior para prueba".toCharArray()
        MigratingCredentialCipher(context, oldPassword).use { cipher ->
            RoomVaultRepository(context, { "demo" }, cipher, database.credentialDao())
                .saveCredential(Credential(name = "Anterior", username = "persona", password = "conservar"))
        }
        val old = database.credentialDao().getAllCredentials("demo").single()
        val pending = accounts.prepare("importar@ejemplo.com", password)
        try {
            try { service.complete(pending, "incorrecta".toCharArray()); fail("Debió rechazar la contraseña anterior") }
            catch (_: IllegalArgumentException) { }
            assertFalse(accounts.hasAccounts())
            assertEquals(old, database.credentialDao().getAllCredentials("demo").single())
            service.complete(pending, oldPassword).use { session ->
                assertTrue(database.credentialDao().getAllCredentials("demo").isEmpty())
                MigratingCredentialCipher(context, session.vaultPassword).use { cipher ->
                    val credential = RoomVaultRepository(context, { session.accountId }, cipher, database.credentialDao()).getCredentials().single()
                    assertEquals(old.id, credential.id)
                    assertEquals("conservar", credential.password)
                }
            }
        } finally { pending.close(); oldPassword.fill('\u0000') }
    }

    @Test fun archivoModificadoNoPermiteEntrarNiRecuperar() = runBlocking<Unit> {
        val pending = accounts.prepare("integridad@ejemplo.com", password)
        val code = pending.recoveryCode
        service.complete(pending, null).close()
        val file = directory.listFiles()!!.single()
        val content = org.json.JSONObject(file.readText())
        val data = android.util.Base64.decode(content.getString("data"), android.util.Base64.NO_WRAP)
        data[0] = (data[0].toInt() xor 1).toByte()
        content.put("data", android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP))
        file.writeText(content.toString())
        fails { accounts.login("integridad@ejemplo.com", password) }
        fails { accounts.recover("integridad@ejemplo.com", code, password) }
    }

    @Test fun unaImportacionInterrumpidaSeCompletaAlEntrar() = runBlocking<Unit> {
        val oldPassword = "Mi acceso anterior".toCharArray()
        MigratingCredentialCipher(context, oldPassword).use { cipher ->
            RoomVaultRepository(context, { "demo" }, cipher, database.credentialDao())
                .saveCredential(Credential(name = "Conservar", username = "persona", password = "no perder"))
        }
        val old = database.credentialDao().getAllCredentials("demo").single()
        val pending = accounts.prepare("reinicio@ejemplo.com", password)
        val session = accounts.commit(pending)
        val fresh = MigratingCredentialCipher(context, oldPassword).use { previous ->
            MigratingCredentialCipher(context, session.vaultPassword).use { current ->
                val payload = current.encrypt(previous.decrypt(old.toPayload()))
                old.copy(ownerId = pending.id, remoteId = null, algorithm = payload.algorithm, kdf = payload.kdf,
                    iterations = payload.iterations, saltB64 = payload.saltB64, nonceB64 = payload.nonceB64,
                    ciphertextB64 = payload.ciphertextB64)
            }
        }
        // Simular el cierre después de escribir la cuenta y antes de cambiar las filas.
        accounts.removeNewAccount(pending)
        accounts.attachImport(pending, listOf(old to fresh))
        accounts.commit(pending).close()
        pending.close(); session.close(); oldPassword.fill('\u0000')
        val restarted = LocalAccessService(context, LocalAccountStore(context, directory, alias), database)
        restarted.login("reinicio@ejemplo.com", password).use { restored ->
            assertEquals(fresh, database.credentialDao().getAllCredentials(restored.accountId).single())
            assertTrue(database.credentialDao().getAllCredentials("demo").isEmpty())
        }
        assertTrue(accounts.pendingImport(fresh.ownerId).isEmpty())
        restarted.login("reinicio@ejemplo.com", password).close()
        assertEquals(1, database.credentialDao().getAllCredentials(fresh.ownerId).size)
    }

    private fun fails(block: () -> Unit) {
        try { block() } catch (_: Exception) { return }
        fail("Debió rechazar el acceso")
    }
}
