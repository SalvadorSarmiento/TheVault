package com.example.thevault.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import com.example.thevault.data.model.Credential
import com.example.thevault.data.model.CredentialCategory
import org.json.JSONObject
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class CredentialCrypto(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val secureRandom = SecureRandom()

    fun encrypt(credential: Credential): EncryptedPayloadDto {
        val salt = randomBytes(SALT_BYTES)
        val nonce = randomBytes(NONCE_BYTES)
        val key = deriveKey(masterSecret(), salt)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
        val ciphertext = cipher.doFinal(credential.toJson().toString().toByteArray(Charsets.UTF_8))

        return EncryptedPayloadDto(
            algorithm = "AES-256-GCM",
            kdf = "PBKDF2-HMAC-SHA256",
            iterations = ITERATIONS,
            saltB64 = salt.toBase64(),
            nonceB64 = nonce.toBase64(),
            ciphertextB64 = ciphertext.toBase64()
        )
    }

    fun decrypt(payload: EncryptedPayloadDto): Credential {
        require(payload.algorithm == "AES-256-GCM") { "Algoritmo no soportado" }
        require(payload.kdf == "PBKDF2-HMAC-SHA256") { "KDF no soportado" }

        val salt = payload.saltB64.fromBase64()
        val nonce = payload.nonceB64.fromBase64()
        val key = deriveKey(masterSecret(), salt, payload.iterations)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
        val plaintext = cipher.doFinal(payload.ciphertextB64.fromBase64())
        return JSONObject(String(plaintext, Charsets.UTF_8)).toCredential()
    }

    @Synchronized
    private fun masterSecret(): ByteArray {
        val storedNonce = preferences.getString(MASTER_NONCE, null)
        val storedSecret = preferences.getString(MASTER_SECRET, null)
        val wrappingKey = getOrCreateWrappingKey()

        if (storedNonce != null && storedSecret != null) {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                wrappingKey,
                GCMParameterSpec(TAG_BITS, storedNonce.fromBase64())
            )
            return cipher.doFinal(storedSecret.fromBase64())
        }

        val secret = randomBytes(MASTER_SECRET_BYTES)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey)
        val encryptedSecret = cipher.doFinal(secret)
        preferences.edit {
            putString(MASTER_NONCE, cipher.iv.toBase64())
            putString(MASTER_SECRET, encryptedSecret.toBase64())
        }
        return secret
    }

    private fun getOrCreateWrappingKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            generateKey()
        }
    }

    private fun deriveKey(
        secret: ByteArray,
        salt: ByteArray,
        iterations: Int = ITERATIONS
    ): SecretKey {
        val password = secret.toBase64().toCharArray()
        val specification = PBEKeySpec(password, salt, iterations, KEY_BITS)
        return try {
            val encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(specification)
                .encoded
            SecretKeySpec(encoded, "AES")
        } finally {
            specification.clearPassword()
            password.fill('\u0000')
        }
    }

    private fun randomBytes(size: Int) = ByteArray(size).also(secureRandom::nextBytes)

    private fun Credential.toJson() = JSONObject()
        .put("name", name)
        .put("username", username)
        .put("password", password)
        .put("website", website)
        .put("notes", notes)
        .put("category", category.name)
        .put("isFavorite", isFavorite)

    private fun JSONObject.toCredential() = Credential(
        name = getString("name"),
        username = getString("username"),
        password = getString("password"),
        website = optString("website"),
        notes = optString("notes"),
        category = runCatching {
            CredentialCategory.valueOf(getString("category"))
        }.getOrDefault(CredentialCategory.PERSONAL),
        isFavorite = optBoolean("isFavorite")
    )

    private fun ByteArray.toBase64(): String =
        Base64.encodeToString(this, Base64.NO_WRAP)

    private fun String.fromBase64(): ByteArray =
        Base64.decode(this, Base64.NO_WRAP)

    private companion object {
        const val PREFERENCES_NAME = "thevault_crypto"
        const val MASTER_SECRET = "master_secret"
        const val MASTER_NONCE = "master_nonce"
        const val KEY_ALIAS = "thevault_master_wrapping_key"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val ITERATIONS = 310_000
        const val KEY_BITS = 256
        const val TAG_BITS = 128
        const val SALT_BYTES = 16
        const val NONCE_BYTES = 12
        const val MASTER_SECRET_BYTES = 32
    }
}
