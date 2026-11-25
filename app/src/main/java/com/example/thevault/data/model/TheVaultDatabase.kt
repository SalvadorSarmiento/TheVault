package com.example.thevault.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.thevault.data.model.Credential
import com.example.thevault.data.model.CredentialCategory
import com.example.thevault.data.security.CredentialCrypto

@Database(
    entities = [CredentialEntity::class, com.example.thevault.data.transfer.VaultImportReceipt::class],
    version = 5,
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
                    .setJournalMode(JournalMode.TRUNCATE)
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        migration3To4(context.applicationContext),
                        MIGRATION_4_5
                    )
                    .build()

                INSTANCE = instance
                instance
            }
        }

        internal val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS vault_import_receipts (id TEXT NOT NULL PRIMARY KEY)")
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

        internal fun migration3To4(context: Context) = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // This PRAGMA returns a row, so Android SQLite requires the query API.
                db.query("PRAGMA secure_delete=ON").use { cursor ->
                    check(cursor.moveToFirst() && cursor.getInt(0) == 1) {
                        "SQLite secure_delete could not be enabled for the migration"
                    }
                }
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS credentials_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        ownerId TEXT NOT NULL DEFAULT 'demo',
                        remoteId TEXT,
                        algorithm TEXT NOT NULL,
                        kdf TEXT NOT NULL,
                        iterations INTEGER NOT NULL,
                        saltB64 TEXT NOT NULL,
                        nonceB64 TEXT NOT NULL,
                        ciphertextB64 TEXT NOT NULL
                    )
                    """.trimIndent()
                )

                CredentialCrypto(context).use { legacyCrypto ->
                    db.query("SELECT * FROM credentials ORDER BY id").use { cursor ->
                        while (cursor.moveToNext()) {
                            val ciphertext = cursor.stringOrNull("ciphertextB64")
                            val payload = if (!ciphertext.isNullOrBlank()) {
                                com.example.thevault.data.remote.EncryptedPayloadDto(
                                    algorithm = cursor.requiredString("algorithm"),
                                    kdf = cursor.requiredString("kdf"),
                                    iterations = cursor.requiredInt("iterations"),
                                    saltB64 = cursor.requiredString("saltB64"),
                                    nonceB64 = cursor.requiredString("nonceB64"),
                                    ciphertextB64 = ciphertext
                                )
                            } else {
                                val category = runCatching {
                                    CredentialCategory.valueOf(cursor.requiredString("category"))
                                }.getOrDefault(CredentialCategory.PERSONAL)
                                legacyCrypto.encrypt(
                                    Credential(
                                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                                        remoteId = cursor.stringOrNull("remoteId"),
                                        name = cursor.requiredString("name"),
                                        username = cursor.requiredString("username"),
                                        password = cursor.requiredString("password"),
                                        website = cursor.requiredString("website"),
                                        notes = cursor.requiredString("notes"),
                                        category = category,
                                        isFavorite = cursor.requiredInt("isFavorite") != 0
                                    )
                                )
                            }

                            db.execSQL(
                                """
                                INSERT INTO credentials_new (
                                    id, ownerId, remoteId, algorithm, kdf, iterations,
                                    saltB64, nonceB64, ciphertextB64
                                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                                """.trimIndent(),
                                arrayOf<Any?>(
                                    cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                                    cursor.requiredString("ownerId"),
                                    cursor.stringOrNull("remoteId"),
                                    payload.algorithm,
                                    payload.kdf,
                                    payload.iterations,
                                    payload.saltB64,
                                    payload.nonceB64,
                                    payload.ciphertextB64
                                )
                            )
                        }
                    }
                }

                db.execSQL("DROP TABLE credentials")
                db.execSQL("ALTER TABLE credentials_new RENAME TO credentials")
            }
        }

        private fun android.database.Cursor.stringOrNull(column: String): String? {
            val index = getColumnIndexOrThrow(column)
            return if (isNull(index)) null else getString(index)
        }

        private fun android.database.Cursor.requiredString(column: String): String =
            getString(getColumnIndexOrThrow(column))

        private fun android.database.Cursor.requiredInt(column: String): Int =
            getInt(getColumnIndexOrThrow(column))
    }
}
