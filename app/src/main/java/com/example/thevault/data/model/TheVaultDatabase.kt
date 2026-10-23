package com.example.thevault.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CredentialEntity::class],
    version = 3,
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
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()

                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE credentials ADD COLUMN ownerId TEXT NOT NULL DEFAULT 'demo'"
                )
                db.execSQL(
                    "ALTER TABLE credentials ADD COLUMN remoteId TEXT"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE credentials ADD COLUMN algorithm TEXT"
                )
                db.execSQL(
                    "ALTER TABLE credentials ADD COLUMN kdf TEXT"
                )
                db.execSQL(
                    "ALTER TABLE credentials ADD COLUMN iterations INTEGER"
                )
                db.execSQL(
                    "ALTER TABLE credentials ADD COLUMN saltB64 TEXT"
                )
                db.execSQL(
                    "ALTER TABLE credentials ADD COLUMN nonceB64 TEXT"
                )
                db.execSQL(
                    "ALTER TABLE credentials ADD COLUMN ciphertextB64 TEXT"
                )
            }
        }
    }
}