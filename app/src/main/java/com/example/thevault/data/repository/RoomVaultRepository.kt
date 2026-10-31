package com.example.thevault.data.repository

import android.content.Context

import com.example.thevault.data.CredentialEntity
import com.example.thevault.data.TheVaultDatabase
import com.example.thevault.data.model.Credential
import com.example.thevault.data.security.CredentialCipher
import com.example.thevault.data.security.CredentialCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RoomVaultRepository(
    context: Context,
    private val ownerIdProvider: () -> String,
    private val crypto: CredentialCipher
) : VaultRepository {

    private val dao = TheVaultDatabase
        .getDatabase(context.applicationContext)
        .credentialDao()

    override suspend fun getCredentials(): List<Credential> {
        val entities = dao.getAllCredentials(ownerIdProvider())
        val decoded = withContext(Dispatchers.Default) {
            entities.map { entity ->
                val payload = entity.toPayload()
                val credential = crypto.decrypt(payload).copy(
                    id = entity.id,
                    remoteId = entity.remoteId
                )
                val replacement = if (payload.kdf != CredentialCrypto.MASTER_PASSWORD_KDF) {
                    encryptToEntity(
                        credential = credential,
                        ownerId = entity.ownerId,
                        id = entity.id,
                        remoteId = entity.remoteId
                    )
                } else {
                    null
                }
                credential to replacement
            }
        }

        decoded.mapNotNull { it.second }.forEach { dao.updateCredential(it) }
        return decoded.map { it.first }
    }

    override suspend fun saveCredential(credential: Credential): Credential {
        val ownerId = ownerIdProvider()
        val existing = credential.id.takeIf { it != 0L }
            ?.let { dao.getCredential(ownerId, it) }
        val entity = withContext(Dispatchers.Default) {
            encryptToEntity(
                credential = credential,
                ownerId = ownerId,
                id = credential.id,
                remoteId = credential.remoteId
            )
        }

        return if (existing != null) {
            dao.updateCredential(entity)
            credential
        } else {
            val generatedId = dao.insertCredential(entity.copy(id = 0))
            credential.copy(id = generatedId)
        }
    }

    override suspend fun deleteCredential(id: Long) {
        val credential = dao.getCredential(ownerIdProvider(), id)
        if (credential != null) {
            dao.deleteCredential(credential)
        }
    }

    private fun encryptToEntity(
        credential: Credential,
        ownerId: String,
        id: Long,
        remoteId: String?
    ): CredentialEntity {
        val payload = crypto.encrypt(credential)

        return CredentialEntity(
            id = id,
            ownerId = ownerId,
            remoteId = remoteId,
            algorithm = payload.algorithm,
            kdf = payload.kdf,
            iterations = payload.iterations,
            saltB64 = payload.saltB64,
            nonceB64 = payload.nonceB64,
            ciphertextB64 = payload.ciphertextB64
        )
    }
}
