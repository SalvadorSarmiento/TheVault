package com.example.thevault

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.thevault.data.remote.CredentialCreationKeys
import java.io.File
import java.io.IOException
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class ClavesCreacionTest {
    private lateinit var directory: File

    @Before fun preparar() {
        directory = File(ApplicationProvider.getApplicationContext<Context>().cacheDir,
            "claves-prueba-${UUID.randomUUID()}").apply { mkdirs() }
    }

    @After fun limpiar() { directory.deleteRecursively() }

    @Test fun conservaLaClaveAlRecrearElRepositorio() {
        val first = CredentialCreationKeys(directory).forCredential(7)
        val restarted = CredentialCreationKeys(directory)
        assertEquals(first, restarted.forCredential(7))
        assertNotEquals(first, restarted.forCredential(8))
    }

    @Test fun noReemplazaUnaClaveDañada() {
        val file = File(directory, "api-installation-id")
        file.writeText("dañada")
        assertThrows(IOException::class.java) { CredentialCreationKeys(directory).forCredential(7) }
        assertEquals("dañada", file.readText())
    }

    @Test fun exigeUnRegistroLocalAntesDeCrearLaClave() {
        assertThrows(IllegalArgumentException::class.java) {
            CredentialCreationKeys(directory).forCredential(0)
        }
        assertFalse(File(directory, "api-installation-id").exists())
    }
}
