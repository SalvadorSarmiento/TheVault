package com.example.thevault.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CredentialDao {

    @Query("SELECT * FROM credentials WHERE ownerId = :ownerId ORDER BY id ASC")
    suspend fun getAllCredentials(ownerId: String): List<CredentialEntity>

    @Query("SELECT * FROM credentials WHERE ownerId = :ownerId AND id = :id LIMIT 1")
    suspend fun getCredential(ownerId: String, id: Long): CredentialEntity?

    @Insert
    suspend fun insertCredential(credential: CredentialEntity): Long

    @Update
    suspend fun updateCredential(credential: CredentialEntity)

    @Delete
    suspend fun deleteCredential(credential: CredentialEntity)
}
