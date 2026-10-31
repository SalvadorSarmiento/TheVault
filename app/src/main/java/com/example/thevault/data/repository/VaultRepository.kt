package com.example.thevault.data.repository

import com.example.thevault.data.model.Credential

interface VaultRepository {
    suspend fun getCredentials(): List<Credential>
    suspend fun saveCredential(credential: Credential): Credential
    suspend fun deleteCredential(id: Long)
}
