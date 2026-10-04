package com.example.thevault.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.thevault.data.model.Credential
import com.example.thevault.data.model.CredentialCategory

@Entity(tableName = "credentials")
data class CredentialEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val username: String,
    val password: String,
    val website: String,
    val notes: String,
    val category: String,
    val isFavorite: Boolean
) {

    fun toCredential(): Credential {
        return Credential(
            id = id,
            name = name,
            username = username,
            password = password,
            website = website,
            notes = notes,
            category = CredentialCategory.valueOf(category),
            isFavorite = isFavorite
        )
    }

    companion object {
        fun fromCredential(credential: Credential): CredentialEntity {
            return CredentialEntity(
                id = credential.id,
                name = credential.name,
                username = credential.username,
                password = credential.password,
                website = credential.website,
                notes = credential.notes,
                category = credential.category.name,
                isFavorite = credential.isFavorite
            )
        }
    }
}