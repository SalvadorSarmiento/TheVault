
package com.example.thevault.data.remote

import android.net.Uri
import com.example.thevault.data.auth.ApiTokenProvider
import com.example.thevault.data.model.Credential
import com.example.thevault.data.security.CredentialCipher
import com.example.thevault.data.security.CredentialCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class RemoteApiException(
    val statusCode: Int,
    method: String,
    path: String,
    val responseBody: String
) : IOException("La API respondió HTTP $statusCode en $method $path")

class HttpRemoteVaultRepository(
    baseUrl: String,
    private val tokenProvider: ApiTokenProvider,
    private val crypto: CredentialCipher,
    private val creationKey: (Long) -> String
) : RemoteVaultRepository {

    private val baseUrl = baseUrl.trimEnd('/').also { value ->
        val endpoint = runCatching { URL(value) }
            .getOrElse { throw IllegalArgumentException("URL de API inválida: $value", it) }
        require(endpoint.protocol == "http" || endpoint.protocol == "https") {
            "La API debe usar HTTP o HTTPS"
        }
        require(endpoint.host.isNotBlank()) { "La URL de la API debe incluir un host" }
    }

    override suspend fun getCredentials(): List<Credential> {
        val response = request(
            method = "GET",
            path = "/api/v1/credentials"
        )

        val items = JSONObject(response.body).getJSONArray("items")

        val decoded = withContext(Dispatchers.Default) {
            buildList {
                for (index in 0 until items.length()) {
                    add(items.getJSONObject(index).toCredential())
                }
            }
        }

        return decoded.map { (credential, payload) ->
            if (payload.kdf != CredentialCrypto.MASTER_PASSWORD_KDF) {
                saveCredential(credential)
            } else {
                credential
            }
        }
    }

    override suspend fun saveCredential(
        credential: Credential
    ): Credential {
        val encryptedPayload = withContext(Dispatchers.Default) {
            crypto.encrypt(credential)
        }

        val body = JSONObject().put(
            "encrypted",
            encryptedPayload.toJson()
        )

        val remoteId = credential.remoteId

        val response = if (remoteId.isNullOrBlank()) {
            val key = withContext(Dispatchers.IO) { creationKey(credential.id) }
            val created = request(
                method = "POST",
                path = "/api/v1/credentials",
                body = body,
                idempotencyKey = key
            )
            if (created.replayed) {
                // El usuario pudo editar la copia local mientras esperaba conexión.
                val id = JSONObject(created.body).getString("id")
                request("PUT", "/api/v1/credentials/${Uri.encode(id)}", body)
            } else created
        } else {
            request(
                method = "PUT",
                path = "/api/v1/credentials/${Uri.encode(remoteId)}",
                body = body
            )
        }

        return withContext(Dispatchers.Default) {
            JSONObject(response.body)
                .toCredential()
                .first
                .copy(id = credential.id)
        }
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
        body: JSONObject? = null,
        idempotencyKey: String? = null
    ): ApiResponse {
        val token = tokenProvider.idToken()

        return withContext(Dispatchers.IO) {
            val connection = URL("$baseUrl$path")
                .openConnection() as HttpURLConnection

            try {
                connection.requestMethod = method
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 15_000
                connection.readTimeout = 15_000
                if (idempotencyKey != null) {
                    connection.setRequestProperty("Idempotency-Key", idempotencyKey)
                }
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
                        ?.use { it.readBoundedUtf8() }
                        .orEmpty()
                } else {
                    connection.errorStream
                        ?.use { it.readBoundedUtf8() }
                        .orEmpty()
                }

                if (status !in 200..299) {
                    throw RemoteApiException(
                        statusCode = status,
                        method = method,
                        path = path,
                        responseBody = responseBody.take(500)
                    )
                }

                ApiResponse(responseBody, connection.getHeaderField("Idempotency-Replayed") == "true")
            } finally {
                connection.disconnect()
            }
        }
    }

    private data class ApiResponse(val body: String, val replayed: Boolean)

    private fun InputStream.readBoundedUtf8(): String {
        val buffer = ByteArray(8192)
        val output = ByteArrayOutputStream()
        while (true) {
            val count = read(buffer)
            if (count == -1) break
            if (output.size() + count > MAX_RESPONSE_BYTES) {
                throw IOException("La respuesta de la API supera el límite de 8 MiB")
            }
            output.write(buffer, 0, count)
        }
        return output.toString(Charsets.UTF_8.name())
    }

    private companion object {
        const val MAX_RESPONSE_BYTES = 8 * 1024 * 1024
    }

    private fun JSONObject.toCredential(): Pair<Credential, EncryptedPayloadDto> {
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

        return crypto.decrypt(payload).copy(remoteId = remoteId) to payload
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
