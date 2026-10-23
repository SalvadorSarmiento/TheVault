package com.example.thevault.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.thevault.data.model.Credential
import com.example.thevault.data.model.CredentialCategory

@Entity(tableName = "credentials")
data class CredentialEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(defaultValue = "'demo'")
    val ownerId: String,

    val remoteId: String?,

    // Columnas antiguas: se conservan para la migración.
    val name: String = "",
    val username: String = "",
    val password: String = "",
    val website: String = "",
    val notes: String = "",
    val category: String = "PERSONAL",
    val isFavorite: Boolean = false,

    // Nuevo contenido cifrado.
    val algorithm: String? = null,
    val kdf: String? = null,
    val iterations: Int? = null,
    val saltB64: String? = null,
    val nonceB64: String? = null,
    val ciphertextB64: String? = null
) {
    fun toLegacyCredential(): Credential {
        return Credential(
            id = id,
            remoteId = remoteId,
            name = name,
            username = username,
            password = password,
            website = website,
            notes = notes,
            category = runCatching {
                CredentialCategory.valueOf(category)
            }.getOrDefault(CredentialCategory.PERSONAL),
            isFavorite = isFavorite
        )
    }

    fun toPayload(): EncryptedPayloadDto {
        return EncryptedPayloadDto(
            algorithm = requireNotNull(algorithm),
            kdf = requireNotNull(kdf),
            iterations = requireNotNull(iterations),
            saltB64 = requireNotNull(saltB64),
            nonceB64 = requireNotNull(nonceB64),
            ciphertextB64 = requireNotNull(ciphertextB64)
        )
    }
}