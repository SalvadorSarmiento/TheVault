package com.example.thevault.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CredentialEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TheVaultDatabase : RoomDatabase() {

    abstract fun credentialDao(): CredentialDao

    companion object {

        @Volatile
        private var INSTANCE: TheVaultDatabase? = null

        fun getDatabase(context: Context): TheVaultDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TheVaultDatabase::class.java,
                    "thevault_database"
                ).build()

                INSTANCE = instance

                instance
            }
        }
    }
}