package com.example.thevault

import android.app.Application
import android.util.Base64
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.example.thevault.data.CredentialEntity
import com.example.thevault.data.TheVaultDatabase
import com.example.thevault.data.auth.LocalAccessService
import com.example.thevault.data.auth.LocalAccountStore
import com.example.thevault.data.model.Credential
import com.example.thevault.data.repository.RoomVaultRepository
import com.example.thevault.data.security.CredentialCrypto
import com.example.thevault.data.transfer.VaultImportReceipt
import com.example.thevault.data.transfer.VaultTransferService
import java.io.File
import java.security.KeyStore
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class TransferenciaBovedaTest {
    @Test fun restauraEnOtroDispositivoSinDuplicarYMantieneLaRecuperacion() = runBlocking<Unit> {
        Device().use { source -> Device().use { target ->
            val account = source.create()
            val packet = source.transfer.export(account.first, EMAIL, PASSWORD.toCharArray())
            assertFalse(String(packet, Charsets.ISO_8859_1).contains(EMAIL))
            assertEquals(EMAIL, target.transfer.restore(packet, account.second, NEW_PASSWORD.toCharArray()))
            val session = target.access.login(EMAIL, NEW_PASSWORD.toCharArray())
            session.use {
                CredentialCrypto.fromMasterPassword(it.vaultPassword).use { cipher ->
                    val rows = RoomVaultRepository(target.app, { it.accountId }, cipher, target.database.credentialDao()).getCredentials()
                    assertEquals(1, rows.size)
                    assertEquals("Correo de prueba", rows.single().name)
                    assertEquals("secreto-ficticio", rows.single().password)
                    assertTrue(rows.single().isFavorite)
                }
            }
            try {
                target.transfer.restore(packet, account.second, NEW_PASSWORD.toCharArray())
                fail("La misma cuenta no debe importarse dos veces")
            } catch (_: IllegalArgumentException) { }
            assertEquals(1, target.database.credentialDao().getAllCredentials(account.first).size)
            assertEquals(1, source.database.credentialDao().getAllCredentials(account.first).size)
            target.access.recover(EMAIL, account.second, "otra-frase-segura-456".toCharArray()).close()
            target.access.login(EMAIL, "otra-frase-segura-456".toCharArray()).close()
        } }
    }

    @Test fun claveIncorrectaOCopiaDañadaNoCreanCuentaNiFilas() = runBlocking<Unit> {
        Device().use { source -> Device().use { target ->
            val account = source.create()
            val packet = source.transfer.export(account.first, EMAIL, PASSWORD.toCharArray())
            for ((copy, code) in listOf(packet to "00".repeat(32),
                packet.copyOf().apply { this[lastIndex] = (last().toInt() xor 1).toByte() } to account.second)) {
                try { target.transfer.restore(copy, code, NEW_PASSWORD.toCharArray()); fail("Debe rechazar la copia") }
                catch (_: IllegalArgumentException) { }
                assertFalse(target.accounts.hasAccounts())
                assertTrue(target.database.credentialDao().getAllCredentials(account.first).isEmpty())
            }
            try { source.transfer.export(account.first, EMAIL, "equivocada".toCharArray()); fail("Debe autenticar el envío") }
            catch (_: IllegalArgumentException) { }
        } }
    }

    @Test fun noSobrescribeUnaCuentaExistenteConElMismoCorreo() = runBlocking<Unit> {
        Device().use { source -> Device().use { target ->
            val first = source.create()
            val existing = target.create()
            val packet = source.transfer.export(first.first, EMAIL, PASSWORD.toCharArray())
            try { target.transfer.restore(packet, first.second, NEW_PASSWORD.toCharArray()); fail("Debe conservar la cuenta existente") }
            catch (_: IllegalArgumentException) { }
            target.access.login(EMAIL, PASSWORD.toCharArray()).use { assertEquals(existing.first, it.accountId) }
            assertEquals(1, target.database.credentialDao().getAllCredentials(existing.first).size)
            assertTrue(target.database.credentialDao().getAllCredentials(first.first).isEmpty())
        } }
    }

    @Test fun retomaUnaImportacionInterrumpidaAntesDeGuardarLasFilas() = runBlocking<Unit> {
        Device().use { target ->
            val prepared = target.stageImport()
            target.access.login(EMAIL, NEW_PASSWORD.toCharArray()).close()
            target.access.login(EMAIL, NEW_PASSWORD.toCharArray()).close()
            assertEquals(1, target.database.credentialDao().getAllCredentials(prepared.first).size)
            assertNull(target.accounts.pendingTransferImport(prepared.first))
        }
    }

    @Test fun unCorteTrasGuardarLasFilasNoDuplicaNiDeshaceEdiciones() = runBlocking<Unit> {
        Device().use { target ->
            val prepared = target.stageImport()
            val journal = checkNotNull(target.accounts.pendingTransferImport(prepared.first))
            target.database.withTransaction {
                target.database.credentialDao().insertCredential(prepared.second)
                target.database.credentialDao().insertImportReceipt(VaultImportReceipt(journal.id))
            }
            val row = target.database.credentialDao().getAllCredentials(prepared.first).single()
            // Simula una edición posterior al commit, antes de poder borrar el diario.
            target.database.credentialDao().updateCredential(row.copy(remoteId = "edicion-conservada"))
            target.access.login(EMAIL, NEW_PASSWORD.toCharArray()).close()
            val restored = target.database.credentialDao().getAllCredentials(prepared.first).single()
            assertEquals("edicion-conservada", restored.remoteId)
            assertNull(target.accounts.pendingTransferImport(prepared.first))
        }
    }

    private class Device : AutoCloseable {
        val app = ApplicationProvider.getApplicationContext<Application>()
        private val unique = UUID.randomUUID().toString()
        private val directory = File(app.cacheDir, "transfer-test-$unique")
        private val alias = "thevault_transfer_test_$unique"
        val database = Room.inMemoryDatabaseBuilder(app, TheVaultDatabase::class.java).build()
        val accounts = LocalAccountStore(app, directory, alias)
        val access = LocalAccessService(app, accounts, database)
        val transfer = VaultTransferService(app, database, access)

        suspend fun create(): Pair<String, String> {
            val pending = accounts.prepare(EMAIL, PASSWORD.toCharArray())
            val code = pending.recoveryCode
            val id = pending.id
            access.complete(pending, null).use { session ->
                CredentialCrypto.fromMasterPassword(session.vaultPassword).use { cipher ->
                    RoomVaultRepository(app, { id }, cipher, database.credentialDao())
                        .saveCredential(Credential(name = "Correo de prueba", username = "prueba",
                            password = "secreto-ficticio", isFavorite = true))
                }
            }
            return id to code
        }

        fun stageImport(): Pair<String, CredentialEntity> {
            val secret = ByteArray(32) { (it + 1).toByte() }
            val recovery = ByteArray(32) { (it + 2).toByte() }
            val id = UUID.randomUUID().toString()
            val pending = accounts.prepareTransferred(id, EMAIL, secret, recovery, NEW_PASSWORD.toCharArray())
            val password = Base64.encodeToString(secret, Base64.NO_WRAP).toCharArray()
            val payload = try { CredentialCrypto.fromMasterPassword(password).use {
                it.encrypt(Credential(name = "Prueba", username = "prueba", password = "ejemplo"))
            } } finally { password.fill('\u0000'); secret.fill(0); recovery.fill(0) }
            val row = CredentialEntity(0, id, null, payload.algorithm, payload.kdf, payload.iterations,
                payload.saltB64, payload.nonceB64, payload.ciphertextB64)
            accounts.attachTransferImport(pending, listOf(row))
            accounts.commit(pending).close()
            pending.close()
            return id to row
        }

        override fun close() {
            database.close()
            directory.deleteRecursively()
            KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(alias) }
        }
    }

    private companion object {
        const val EMAIL = "transferencia@example.com"
        const val PASSWORD = "frase-original-prueba-123"
        const val NEW_PASSWORD = "frase-nueva-prueba-789"
    }
}
