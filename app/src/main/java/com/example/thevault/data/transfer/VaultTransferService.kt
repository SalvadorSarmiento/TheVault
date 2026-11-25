package com.example.thevault.data.transfer

import android.content.Context
import android.util.Base64
import com.example.thevault.data.CredentialEntity
import com.example.thevault.data.TheVaultDatabase
import com.example.thevault.data.auth.LocalAccessService
import com.example.thevault.data.auth.LocalAccountStore
import com.example.thevault.data.security.CredentialCrypto
import org.json.JSONArray
import org.json.JSONObject

class VaultTransferService(
    context: Context,
    private val database: TheVaultDatabase = TheVaultDatabase.getDatabase(context),
    private val access: LocalAccessService = LocalAccessService(context, database = database)
) {
    suspend fun export(accountId: String, email: String, password: CharArray): ByteArray {
        access.accounts.transferIdentity(accountId, email, password).use { identity ->
            val rows = database.credentialDao().getAllCredentials(accountId)
            require(rows.size <= MAX_ROWS) { "Esta beta permite transferir hasta $MAX_ROWS credenciales." }
            require(rows.all { it.kdf == CredentialCrypto.MASTER_PASSWORD_KDF }) {
                "Abre tu bóveda antes de transferirla para actualizar su cifrado."
            }
            val content = JSONObject().put("version", 1).put("accountId", identity.id)
                .put("email", identity.email).put("secret", encode(identity.secret))
                .put("rows", JSONArray().apply { rows.forEach { put(rowJson(it)) } })
                .toString().toByteArray(Charsets.UTF_8)
            return try { TransferEnvelope.encrypt(content, identity.recovery) }
            finally { content.fill(0) }
        }
    }

    suspend fun restore(packet: ByteArray, recoveryCode: String, password: CharArray): String {
        LocalAccountStore.requirePassword(password)
        val recovery = LocalAccountStore.decodeRecovery(recoveryCode)
        try {
            val plain = try { TransferEnvelope.decrypt(packet, recovery) }
            catch (_: Exception) { throw IllegalArgumentException("La clave no corresponde a esta copia o los datos están dañados.") }
            val content = try { JSONObject(String(plain, Charsets.UTF_8)) } finally { plain.fill(0) }
            require(content.getInt("version") == 1) { "Copia no compatible." }
            val id = content.getString("accountId")
            val email = content.getString("email")
            val encoded = content.getString("secret")
            require(encoded.length == 44) { "Copia inválida." }
            val secret = Base64.decode(encoded, Base64.NO_WRAP)
            try {
                require(secret.size == 32) { "Copia inválida." }
                val array = content.getJSONArray("rows")
                require(array.length() <= MAX_ROWS) { "La copia contiene demasiadas credenciales." }
                val rows = (0 until array.length()).map { parseRow(array.getJSONObject(it), id) }
                // Comprobar todo antes de crear la cuenta o escribir una sola fila.
                val vaultPassword = encode(secret).toCharArray()
                try {
                    CredentialCrypto.fromMasterPassword(vaultPassword).use { cipher ->
                        rows.forEach { cipher.decrypt(it.toPayload()) }
                    }
                } finally { vaultPassword.fill('\u0000') }
                val pending = access.accounts.prepareTransferred(id, email, secret, recovery, password)
                try { access.completeTransfer(pending, rows).close() } finally { pending.close() }
                return LocalAccountStore.normalizeEmail(email)
            } finally { secret.fill(0) }
        } finally { recovery.fill(0) }
    }

    private fun rowJson(row: CredentialEntity) = JSONObject()
        .put("remoteId", row.remoteId ?: JSONObject.NULL).put("algorithm", row.algorithm)
        .put("kdf", row.kdf).put("iterations", row.iterations).put("salt", row.saltB64)
        .put("nonce", row.nonceB64).put("data", row.ciphertextB64)

    private fun parseRow(row: JSONObject, owner: String): CredentialEntity {
        require(row.getString("algorithm") == "AES-256-GCM" &&
            row.getString("kdf") == CredentialCrypto.MASTER_PASSWORD_KDF) { "Cifrado no compatible." }
        // Conserva el rango admitido por el cifrado local y limita su coste.
        require(row.getInt("iterations") in CredentialCrypto.ITERATIONS..1_000_000) { "Copia no compatible." }
        val remote = if (row.isNull("remoteId")) null else row.getString("remoteId")
        require(remote == null || remote.length in 1..1024)
        return CredentialEntity(0, owner, remote, row.getString("algorithm"), row.getString("kdf"),
            row.getInt("iterations"), row.getString("salt"), row.getString("nonce"), row.getString("data"))
    }

    private fun encode(value: ByteArray) = Base64.encodeToString(value, Base64.NO_WRAP)
    private companion object { const val MAX_ROWS = 1000 }
}
