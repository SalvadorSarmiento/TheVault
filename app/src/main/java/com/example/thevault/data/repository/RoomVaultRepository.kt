package com.example.thevault.data.repository

import android.content.Context

import com.example.thevault.data.CredentialEntity
import com.example.thevault.data.TheVaultDatabase
import com.example.thevault.data.model.Credential
import com.example.thevault.data.security.CredentialCrypto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class RoomVaultRepository(
    context: Context,
    private val ownerIdProvider: () -> String
) : VaultRepository {

    private val dao = TheVaultDatabase
        .getDatabase(context.applicationContext)
        .credentialDao()

    private val crypto = CredentialCrypto(context.applicationContext)

    override fun getCredentials(): List<Credential> {
        return runBlocking {
            val ownerId = ownerIdProvider()

            dao.getAllCredentials(ownerId)
                .first()
                .map { entity ->
                    if (!entity.ciphertextB64.isNullOrBlank()) {
                        // Registro actual ya cifrado.
                        crypto.decrypt(entity.toPayload())
                            .copy(
                                id = entity.id,
                                remoteId = entity.remoteId
                            )
                    } else {
                        // Registro antiguo para conversion.
                        val credential = entity.toLegacyCredential()
                        val encryptedEntity = encryptToEntity(
                            credential = credential,
                            ownerId = entity.ownerId,
                            id = entity.id,
                            remoteId = entity.remoteId
                        )

                        dao.updateCredential(encryptedEntity)

                        credential
                    }
                }
        }
    }

    override fun saveCredential(credential: Credential) {
        runBlocking {
            val ownerId = ownerIdProvider()

            val existing = dao.getAllCredentials(ownerId)
                .first()
                .any { it.id == credential.id && credential.id != 0L }

            val entity = encryptToEntity(
                credential = credential,
                ownerId = ownerId,
                id = credential.id,
                remoteId = credential.remoteId
            )

            if (existing) {
                dao.updateCredential(entity)
            } else {
                dao.insertCredential(entity)
            }
        }
    }

    override fun deleteCredential(id: Long) {
        runBlocking {
            val ownerId = ownerIdProvider()

            val credential = dao.getAllCredentials(ownerId)
                .first()
                .firstOrNull { it.id == id }

            if (credential != null) {
                dao.deleteCredential(credential)
            }
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

            // MANTENER VACIO, nada de datos para evitar data sensible expuesta.
            name = "",
            username = "",
            password = "",
            website = "",
            notes = "",
            category = "PERSONAL",
            isFavorite = false,

            algorithm = payload.algorithm,
            kdf = payload.kdf,
            iterations = payload.iterations,
            saltB64 = payload.saltB64,
            nonceB64 = payload.nonceB64,
            ciphertextB64 = payload.ciphertextB64
        )
    }
}