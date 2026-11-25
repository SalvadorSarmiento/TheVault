package com.example.thevault.data.remote

import android.util.AtomicFile
import java.io.File
import java.io.IOException
import java.util.UUID

/** Conserva la identidad de cada creación aunque se cierre la app o se pierda la respuesta. */
class CredentialCreationKeys(private val directory: File) {
    fun forCredential(localId: Long): String = synchronized(lock) {
        require(localId > 0) { "La credencial debe guardarse localmente primero" }
        val file = AtomicFile(File(directory, "api-installation-id"))
        val installationId = if (file.baseFile.exists() || File(directory, "api-installation-id.bak").exists()) {
            val value = file.openRead().use { input ->
                val bytes = ByteArray(37)
                var size = 0
                while (size < bytes.size) {
                    val count = input.read(bytes, size, bytes.size - size)
                    if (count == -1) break
                    size += count
                }
                if (size != 36) throw IOException("Identificador de instalación inválido")
                String(bytes, 0, size, Charsets.US_ASCII)
            }
            if (runCatching { UUID.fromString(value).toString() }.getOrNull() != value) {
                throw IOException("Identificador de instalación inválido")
            }
            value
        } else {
            val value = UUID.randomUUID().toString()
            val output = file.startWrite()
            try {
                output.write(value.toByteArray(Charsets.US_ASCII))
                file.finishWrite(output)
            } catch (error: Exception) {
                file.failWrite(output)
                throw error
            }
            value
        }
        "$installationId:$localId"
    }

    private companion object {
        val lock = Any()
    }
}
