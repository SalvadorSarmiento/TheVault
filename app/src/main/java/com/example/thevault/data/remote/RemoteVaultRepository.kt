package com.example.thevault.data.remote

import com.example.thevault.data.model.Credential

interface RemoteVaultRepository {
    suspend fun getCredentials(): List<Credential>

    suspend fun saveCredential(
        credential: Credential
    ): Credential

    suspend fun deleteCredential(
        remoteId: String
    )
}