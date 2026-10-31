package com.example.thevault.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.thevault.data.remote.EncryptedPayloadDto

@Entity(tableName = "credentials")
data class CredentialEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(defaultValue = "'demo'")
    val ownerId: String,
    val remoteId: String?,
    val algorithm: String,
    val kdf: String,
    val iterations: Int,
    val saltB64: String,
    val nonceB64: String,
    val ciphertextB64: String
) {
    fun toPayload() = EncryptedPayloadDto(
        algorithm = algorithm,
        kdf = kdf,
        iterations = iterations,
        saltB64 = saltB64,
        nonceB64 = nonceB64,
        ciphertextB64 = ciphertextB64
    )
}
