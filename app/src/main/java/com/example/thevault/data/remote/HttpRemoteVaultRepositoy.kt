
package com.example.thevault.data.remote

import android.net.Uri
import com.example.thevault.data.auth.ApiTokenProvider
import com.example.thevault.data.model.Credential
import com.example.thevault.data.security.CredentialCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class HttpRemoteVaultRepository(
    baseUrl: String,
    private val tokenProvider: ApiTokenProvider,
    private val crypto: CredentialCrypto
) : RemoteVaultRepository {

    private val baseUrl = baseUrl.trimEnd('/')

    override suspend fun getCredentials(): List<Credential> {
        val response = request(
            method = "GET",
            path = "/api/v1/credentials"
        )

        val items = JSONObject(response).getJSONArray("items")

        return buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                add(item.toCredential())
            }
        }
    }

    override suspend fun saveCredential(
        credential: Credential
    ): Credential {
        val encryptedPayload = crypto.encrypt(credential)

        val body = JSONObject().put(
            "encrypted",
            encryptedPayload.toJson()
        )

        val remoteId = credential.remoteId

        val response = if (remoteId.isNullOrBlank()) {
            request(
                method = "POST",
                path = "/api/v1/credentials",
                body = body
            )
        } else {
            request(
                method = "PUT",
                path = "/api/v1/credentials/${Uri.encode(remoteId)}",
                body = body
            )
        }

        return JSONObject(response)
            .toCredential()
            .copy(id = credential.id)
    }

    override suspend fun deleteCredential(remoteId: String) {
        require(remoteId.isNotBlank()) {
            "No se puede eliminar una credencial sin ID remoto"
        }

        request(
            method = "DELETE",
            path = "/api/v1/credentials/${Uri.encode(remoteId)}"
        )
    }

    private suspend fun request(
        method: String,
        path: String,
        body: JSONObject? = null
    ): String {
        val token = tokenProvider.idToken()

        return withContext(Dispatchers.IO) {
            val connection = URL("$baseUrl$path")
                .openConnection() as HttpURLConnection

            try {
                connection.requestMethod = method
                connection.connectTimeout = 15_000
                connection.readTimeout = 15_000
                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $token"
                )
                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                )

                if (body != null) {
                    connection.doOutput = true
                    connection.setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                    )

                    connection.outputStream.use { output ->
                        output.write(
                            body.toString().toByteArray(Charsets.UTF_8)
                        )
                    }
                }

                val status = connection.responseCode
                val responseBody = if (status in 200..299) {
                    connection.inputStream
                        ?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                        .orEmpty()
                } else {
                    connection.errorStream
                        ?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                        .orEmpty()
                }

                if (status !in 200..299) {
                    error(
                        "Error HTTP $status en $method $path: " +
                                responseBody.take(500)
                    )
                }

                responseBody
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun JSONObject.toCredential(): Credential {
        val remoteId = getString("id")
        val encrypted = getJSONObject("encrypted")

        val payload = EncryptedPayloadDto(
            algorithm = encrypted.getString("algorithm"),
            kdf = encrypted.getString("kdf"),
            iterations = encrypted.getInt("iterations"),
            saltB64 = encrypted.getString("saltB64"),
            nonceB64 = encrypted.getString("nonceB64"),
            ciphertextB64 = encrypted.getString("ciphertextB64")
        )

        return crypto.decrypt(payload).copy(
            remoteId = remoteId
        )
    }

    private fun EncryptedPayloadDto.toJson(): JSONObject =
        JSONObject()
            .put("algorithm", algorithm)
            .put("kdf", kdf)
            .put("iterations", iterations)
            .put("saltB64", saltB64)
            .put("nonceB64", nonceB64)
            .put("ciphertextB64", ciphertextB64)
}
