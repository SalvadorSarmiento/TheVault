package com.example.thevault.data.auth

import android.content.Context
import androidx.room.withTransaction
import com.example.thevault.data.CredentialEntity
import com.example.thevault.data.TheVaultDatabase
import com.example.thevault.data.security.MigratingCredentialCipher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class LocalAccessService(
    private val context: Context,
    val accounts: LocalAccountStore = LocalAccountStore(context),
    private val database: TheVaultDatabase = TheVaultDatabase.getDatabase(context)
) {
    suspend fun hasLegacyVault() = database.credentialDao().getAllCredentials("demo").isNotEmpty()

    suspend fun login(email: String, password: CharArray): LocalSession = resume(accounts.login(email, password))
    suspend fun recover(email: String, code: String, password: CharArray): LocalSession =
        resume(accounts.recover(email, code, password))

    private suspend fun resume(session: LocalSession): LocalSession {
        try { finishImport(session.accountId); return session }
        catch (error: Exception) { session.close(); throw error }
    }

    private suspend fun finishImport(accountId: String) = withContext(NonCancellable) {
        val rows = accounts.pendingImport(accountId)
        if (rows.isEmpty()) return@withContext
        database.withTransaction {
            rows.forEach { (old, fresh) ->
                check(old.ownerId == "demo" && fresh.ownerId == accountId && fresh.id == old.id)
                val existing = database.credentialDao().getCredential("demo", old.id)
                if (existing != null) {
                    check(existing == old) { "La bóveda anterior cambió. No se modificaron tus datos." }
                    database.credentialDao().updateCredential(fresh)
                } else {
                    check(database.credentialDao().getCredential(accountId, fresh.id) == fresh) {
                        "No se pudo completar la importación anterior. No borres los datos de la app."
                    }
                }
            }
        }
        // No se abre la bóveda hasta cerrar el diario; un reinicio reintenta este paso.
        accounts.clearImport(accountId)
    }

    suspend fun complete(pending: PendingAccount, oldPassword: CharArray?): LocalSession = withContext(NonCancellable) {
        val replacements = if (oldPassword == null) emptyList() else {
            val rows = database.credentialDao().getAllCredentials("demo")
            val newSecret = android.util.Base64.encodeToString(pending.secret, android.util.Base64.NO_WRAP).toCharArray()
            try {
                MigratingCredentialCipher(context, oldPassword).use { old ->
                    MigratingCredentialCipher(context, newSecret).use { fresh ->
                        rows.map { row ->
                            val payload = fresh.encrypt(old.decrypt(row.toPayload()))
                            row to CredentialEntity(row.id, pending.id, null, payload.algorithm, payload.kdf,
                                payload.iterations, payload.saltB64, payload.nonceB64, payload.ciphertextB64)
                        }
                    }
                }
            } catch (_: Exception) {
                throw IllegalArgumentException("No se pudo abrir la bóveda anterior. Revisa su contraseña; tus datos siguen guardados.")
            } finally { newSecret.fill('\u0000') }
        }
        accounts.attachImport(pending, replacements)
        val session = accounts.commit(pending)
        try {
            finishImport(session.accountId)
            pending.close()
            session
        } catch (error: Exception) {
            session.close()
            if (database.credentialDao().getAllCredentials(pending.id).isEmpty()) {
                accounts.removeNewAccount(pending)
            } else {
                pending.close()
                throw IllegalStateException("La cuenta se creó y tus datos están guardados. Vuelve a iniciar sesión para terminar.", error)
            }
            throw error
        }
    }
}
