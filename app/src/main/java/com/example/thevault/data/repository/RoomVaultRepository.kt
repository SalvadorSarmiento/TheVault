package com.example.thevault.data.repository

import android.content.Context
import com.example.thevault.data.CredentialEntity
import com.example.thevault.data.TheVaultDatabase
import com.example.thevault.data.model.Credential
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class RoomVaultRepository(
    context: Context,
    private val ownerIdProvider: () -> String
) : VaultRepository {

    private val dao = TheVaultDatabase
        .getDatabase(context)
        .credentialDao()

    override fun getCredentials(): List<Credential> {
        return runBlocking {
            dao.getAllCredentials(ownerIdProvider())
                .first()
                .map { it.toCredential() }
        }
    }

    override fun saveCredential(credential: Credential) {
        runBlocking {
            val ownerId = ownerIdProvider()
            val entity = CredentialEntity.fromCredential(credential, ownerId)

            val existing = dao.getAllCredentials(ownerId)
                .first()
                .any { it.id == credential.id }

            if (existing) {
                dao.updateCredential(entity)
            } else {
                dao.insertCredential(entity)
            }
        }
    }

    override fun deleteCredential(id: Long) {
        runBlocking {
            val credential = dao.getAllCredentials(ownerIdProvider())
                .first()
                .firstOrNull { it.id == id }

            if (credential != null) {
                dao.deleteCredential(credential)
            }
        }
    }
}
