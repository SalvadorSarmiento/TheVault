package com.example.thevault.data.repository

import com.example.thevault.data.model.Credential

interface VaultRepository {
    fun getCredentials(): List<Credential>
    fun saveCredential(credential: Credential)
    fun deleteCredential(id: Long)
}