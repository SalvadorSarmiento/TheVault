package com.example.thevault.data.repository

import com.example.thevault.data.model.Credential
import com.example.thevault.data.model.CredentialCategory

interface VaultRepository {
    fun getCredentials(): List<Credential>
    fun saveCredential(credential: Credential)
    fun deleteCredential(id: Long)
}

/**
 * Fuente de datos temporal para demostrar las vistas. Se reemplazará por la
 * implementación de persistencia cuando se desarrolle el CRUD real.
 */
class FakeVaultRepository : VaultRepository {
    private val credentials = mutableListOf(
        Credential(
            id = 1,
            name = "Correo personal",
            username = "ana@ejemplo.com",
            password = "ClaveSegura#2026",
            website = "correo.ejemplo.com",
            notes = "Cuenta de uso personal",
            category = CredentialCategory.PERSONAL,
            isFavorite = true
        ),
        Credential(
            id = 2,
            name = "Cuenta de trabajo",
            username = "ana.trabajo@ejemplo.com",
            password = "Trabajo#2026!",
            website = "intranet.ejemplo.com",
            category = CredentialCategory.WORK
        ),
        Credential(
            id = 3,
            name = "Universidad",
            username = "alumna2026",
            password = "Estudios#2026",
            category = CredentialCategory.STUDIES,
            isFavorite = true
        ),
        Credential(
            id = 4,
            name = "Compras",
            username = "ana@ejemplo.com",
            password = "Compras#2026",
            category = CredentialCategory.SHOPPING
        )
    )

    override fun getCredentials(): List<Credential> = credentials.toList()

    override fun saveCredential(credential: Credential) {
        val index = credentials.indexOfFirst { it.id == credential.id }
        if (index >= 0) credentials[index] = credential else credentials += credential
    }

    override fun deleteCredential(id: Long) {
        credentials.removeAll { it.id == id }
    }
}
