package com.example.thevault

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.thevault.data.TheVaultDatabase
import com.example.thevault.data.model.Credential
import com.example.thevault.data.model.CredentialCategory
import com.example.thevault.data.security.CredentialCrypto
import java.security.GeneralSecurityException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VaultStorageInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun masterPasswordDecryptsOnAnotherCipherInstance() {
        val credential = Credential(
            name = "Correo",
            username = "ana@ejemplo.com",
            password = "secreto",
            website = "https://ejemplo.com",
            notes = "cuenta principal",
            category = CredentialCategory.PERSONAL,
            isFavorite = true
        )
        val password = "correct horse battery staple".toCharArray()
        val payload = CredentialCrypto.fromMasterPassword(password).use { cipher ->
            cipher.encrypt(credential)
        }

        val restored = CredentialCrypto.fromMasterPassword(password).use { cipher ->
            cipher.decrypt(payload)
        }

        assertEquals(credential, restored)
        assertThrows(GeneralSecurityException::class.java) {
            CredentialCrypto.fromMasterPassword("otra contraseña".toCharArray()).use { cipher ->
                cipher.decrypt(payload)
            }
        }
        password.fill('\u0000')
    }

    @Test
    fun migrationFromVersion3RemovesPlaintextColumnsAndPreservesData() = runBlocking {
        val databaseName = "migration-${System.nanoTime()}.db"
        val secret = "clave-super-secreta"
        context.deleteDatabase(databaseName)

        context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { oldDb ->
            oldDb.execSQL(
                """
                CREATE TABLE credentials (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    ownerId TEXT NOT NULL DEFAULT 'demo',
                    remoteId TEXT,
                    name TEXT NOT NULL,
                    username TEXT NOT NULL,
                    password TEXT NOT NULL,
                    website TEXT NOT NULL,
                    notes TEXT NOT NULL,
                    category TEXT NOT NULL,
                    isFavorite INTEGER NOT NULL,
                    algorithm TEXT,
                    kdf TEXT,
                    iterations INTEGER,
                    saltB64 TEXT,
                    nonceB64 TEXT,
                    ciphertextB64 TEXT
                )
                """.trimIndent()
            )
            oldDb.execSQL(
                """
                INSERT INTO credentials (
                    ownerId, remoteId, name, username, password, website,
                    notes, category, isFavorite
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>("demo", null, "Correo", "ana", secret, "", "", "PERSONAL", 1)
            )
            oldDb.version = 3
        }

        val database = Room.databaseBuilder(context, TheVaultDatabase::class.java, databaseName)
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .addMigrations(TheVaultDatabase.migration3To4(context))
            .build()

        val entity = database.credentialDao().getAllCredentials("demo").single()
        val restored = CredentialCrypto(context).use { cipher ->
            cipher.decrypt(entity.toPayload())
        }
        val columns = database.openHelper.readableDatabase
            .query("PRAGMA table_info(credentials)")
            .use { cursor ->
                buildSet {
                    val nameIndex = cursor.getColumnIndexOrThrow("name")
                    while (cursor.moveToNext()) add(cursor.getString(nameIndex))
                }
            }

        assertEquals(secret, restored.password)
        assertFalse("password" in columns)
        assertFalse("username" in columns)
        database.close()

        val rawDatabase = context.getDatabasePath(databaseName)
            .readBytes()
            .toString(Charsets.ISO_8859_1)
        assertFalse(rawDatabase.contains(secret))
        context.deleteDatabase(databaseName)
    }
}
