package com.example.thevault.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CredentialDao {

    @Query("SELECT * FROM credentials ORDER BY name ASC")
    fun getAllCredentials(): Flow<List<CredentialEntity>>

    @Insert
    suspend fun insertCredential(credential: CredentialEntity)

    @Update
    suspend fun updateCredential(credential: CredentialEntity)

    @Delete
    suspend fun deleteCredential(credential: CredentialEntity)
}