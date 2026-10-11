package com.example.thevault.data.remote

import com.example.thevault.data.auth.ApiTokenProvider
import com.example.thevault.data.model.Credential
import com.example.thevault.data.security.CredentialCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class EncryptedPayloadDto(
    val algorithm: String,
    val kdf: String,
    val iterations: Int,
    val saltB64: String,
    val nonceB64: String,
    val ciphertextB64: String
)

interface RemoteVaultRepository {
    suspend fun getCredentials(): List<Credential>
    suspend fun saveCredential(credential: Credential): Credential
    suspend fun deleteCredential(remoteId: String)
}

class HttpRemoteVaultRepository(
    baseUrl: String,
    private val tokenProvider: ApiTokenProvider,
    private val crypto: CredentialCrypto
) : RemoteVaultRepository {
    private val baseUrl = baseUrl.trimEnd('/')

    override suspend fun getCredentials(): List<Credential> {
        val response = request("GET", "/credentials")
        val items = JSONObject(response).getJSONArray("items")
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                add(
                    crypto.decrypt(item.getJSONObject("encrypted").toEncryptedPayload())
                        .copy(remoteId = item.getString("id"))
                )
            }
        }
    }

    override suspend fun saveCredential(credential: Credential): Credential {
        val payload = crypto.encrypt(credential)
        val body = JSONObject().put("encrypted", payload.toJson()).toString()
        val method = if (credential.remoteId == null) "POST" else "PUT"
        val path = credential.remoteId?.let { "/credentials/$it" } ?: "/credentials"
        val response = request(method, path, body)
        return credential.copy(remoteId = JSONObject(response).getString("id"))
    }

    override suspend fun deleteCredential(remoteId: String) {
        request("DELETE", "/credentials/$remoteId")
    }

    private suspend fun request(method: String, path: String, body: String? = null): String {
        val token = tokenProvider.idToken()
        return withContext(Dispatchers.IO) {
            val connection = URL("$baseUrl$path").openConnection() as HttpURLConnection
            try {
                connection.requestMethod = method
                connection.connectTimeout = TIMEOUT_MILLIS
                connection.readTimeout = TIMEOUT_MILLIS
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Accept", "application/json")

                if (body != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
                }

                val statusCode = connection.responseCode
                val stream = if (statusCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
                val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (statusCode !in 200..299) {
                    error("API HTTP $statusCode: $response")
                }
                response
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun EncryptedPayloadDto.toJson() = JSONObject()
        .put("algorithm", algorithm)
        .put("kdf", kdf)
        .put("iterations", iterations)
        .put("saltB64", saltB64)
        .put("nonceB64", nonceB64)
        .put("ciphertextB64", ciphertextB64)

    private fun JSONObject.toEncryptedPayload() = EncryptedPayloadDto(
        algorithm = getString("algorithm"),
        kdf = getString("kdf"),
        iterations = getInt("iterations"),
        saltB64 = getString("saltB64"),
        nonceB64 = getString("nonceB64"),
        ciphertextB64 = getString("ciphertextB64")
    )

    private companion object {
        const val TIMEOUT_MILLIS = 15_000
    }
}
