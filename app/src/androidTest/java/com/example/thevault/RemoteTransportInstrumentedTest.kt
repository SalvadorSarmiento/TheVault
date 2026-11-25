package com.example.thevault

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.thevault.data.auth.ApiTokenProvider
import com.example.thevault.data.model.Credential
import com.example.thevault.data.remote.EncryptedPayloadDto
import com.example.thevault.data.remote.HttpRemoteVaultRepository
import com.example.thevault.data.remote.RemoteApiException
import com.example.thevault.data.security.CredentialCipher
import java.io.IOException
import java.net.ServerSocket
import kotlin.concurrent.thread
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemoteTransportInstrumentedTest {
    private val unusedCipher = object : CredentialCipher {
        override fun encrypt(credential: Credential): EncryptedPayloadDto = error("Not used")
        override fun decrypt(payload: EncryptedPayloadDto): Credential = error("Not used")
    }

    @Test
    fun doesNotFollowAuthenticatedRedirects() = runBlocking<Unit> {
        withServer { server, repository ->
            val responder = thread(isDaemon = true) {
                server.accept().use { socket ->
                    val input = socket.getInputStream().bufferedReader()
                    while (!input.readLine().isNullOrEmpty()) { /* consume HTTP headers */ }
                    socket.getOutputStream().write(
                        ("HTTP/1.1 302 Found\r\nLocation: http://localhost:${server.localPort}/elsewhere\r\n" +
                            "Content-Length: 0\r\nConnection: close\r\n\r\n").toByteArray()
                    )
                }
            }
            try {
                repository.getCredentials()
                fail("Redirect must be returned as an API error")
            } catch (error: RemoteApiException) {
                assertEquals(302, error.statusCode)
            } finally {
                responder.join(2_000)
            }
        }
    }

    @Test
    fun rejectsOversizedResponses() = runBlocking<Unit> {
        withServer { server, repository ->
            val responder = thread(isDaemon = true) {
                server.accept().use { socket ->
                    val input = socket.getInputStream().bufferedReader()
                    while (!input.readLine().isNullOrEmpty()) { /* consume HTTP headers */ }
                    val output = socket.getOutputStream()
                    val size = 8 * 1024 * 1024 + 1
                    output.write("HTTP/1.1 200 OK\r\nContent-Length: $size\r\nConnection: close\r\n\r\n".toByteArray())
                    try {
                        val chunk = ByteArray(8192) { 'A'.code.toByte() }
                        repeat(size / chunk.size) { output.write(chunk) }
                        output.write('A'.code)
                    } catch (_: IOException) {
                        // The client may close the connection as soon as its limit is reached.
                    }
                }
            }
            try {
                repository.getCredentials()
                fail("Oversized response must be rejected")
            } catch (error: IOException) {
                assertTrue(error.message.orEmpty().contains("8 MiB"))
            } finally {
                responder.join(2_000)
            }
        }
    }

    private suspend fun withServer(block: suspend (ServerSocket, HttpRemoteVaultRepository) -> Unit) {
        ServerSocket(0).use { server ->
            server.soTimeout = 10_000
            val repository = HttpRemoteVaultRepository(
                "http://localhost:${server.localPort}",
                object : ApiTokenProvider { override suspend fun idToken() = "test-token" },
                unusedCipher,
                { id -> "prueba:$id" }
            )
            block(server, repository)
        }
    }
}
