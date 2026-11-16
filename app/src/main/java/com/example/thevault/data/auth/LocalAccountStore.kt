package com.example.thevault.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import android.util.Patterns
import com.example.thevault.data.CredentialEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Locale
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class LocalSession(val accountId: String, val vaultPassword: CharArray) : AutoCloseable {
    override fun close() = vaultPassword.fill('\u0000')
}

class PendingAccount internal constructor(
    val id: String,
    val email: String,
    val recoveryCode: String,
    internal val record: JSONObject,
    internal val secret: ByteArray
) : AutoCloseable {
    internal var consumed = false
    override fun close() { secret.fill(0); consumed = true }
}

/** Los archivos contienen un sobre AES-256-GCM; nunca la contraseña ni la clave en claro. */
class LocalAccountStore(
    context: Context,
    private val directory: File = File(context.noBackupFilesDir, "local_accounts"),
    private val keyAlias: String = "thevault_local_accounts_v1"
) {
    private val random = SecureRandom()

    @Synchronized fun hasAccounts(): Boolean = files().isNotEmpty()

    @Synchronized fun prepare(email: String, password: CharArray): PendingAccount {
        val normalized = normalizeEmail(email)
        requirePassword(password)
        require(find(normalized) == null) { "Ya existe una cuenta con ese correo en este dispositivo." }
        val id = UUID.randomUUID().toString()
        val secret = bytes(32)
        val recovery = bytes(32)
        try {
            val code = recovery.joinToString("") { "%02X".format(it.toInt() and 255) }.chunked(8).joinToString("-")
            val record = JSONObject().put("version", 1).put("id", id).put("email", normalized)
            wrapPassword(record, password, secret, recovery)
            record.put("recovery", seal(SecretKeySpec(recovery, "AES"), secret, aad(record, "recovery")))
            return PendingAccount(id, normalized, code, record, secret)
        } catch (error: Exception) {
            secret.fill(0)
            throw error
        } finally { recovery.fill(0) }
    }

    @Synchronized fun commit(pending: PendingAccount): LocalSession {
        check(!pending.consumed) { "Vuelve a crear la cuenta." }
        require(find(pending.email) == null) { "Ya existe una cuenta con ese correo." }
        write(pending.record)
        return session(pending.record, pending.secret)
    }

    @Synchronized fun removeNewAccount(pending: PendingAccount) {
        AtomicFile(file(pending.id)).delete()
    }

    // El diario guarda solo filas ya cifradas. Permite continuar una importación interrumpida.
    internal fun attachImport(pending: PendingAccount, rows: List<Pair<CredentialEntity, CredentialEntity>>) {
        if (rows.isNotEmpty()) pending.record.put("legacyImport", JSONArray().apply {
            rows.forEach { (old, fresh) -> put(JSONObject().put("old", rowJson(old)).put("fresh", rowJson(fresh))) }
        })
    }

    @Synchronized internal fun pendingImport(id: String): List<Pair<CredentialEntity, CredentialEntity>> {
        val array = read(file(id)).optJSONArray("legacyImport") ?: return emptyList()
        return (0 until array.length()).map { index ->
            val pair = array.getJSONObject(index)
            parseRow(pair.getJSONObject("old")) to parseRow(pair.getJSONObject("fresh"))
        }
    }

    @Synchronized internal fun clearImport(id: String) {
        val record = read(file(id))
        record.remove("legacyImport")
        write(record)
    }

    private fun rowJson(row: CredentialEntity) = JSONObject().put("id", row.id).put("owner", row.ownerId)
        .put("remote", row.remoteId ?: JSONObject.NULL).put("algorithm", row.algorithm).put("kdf", row.kdf)
        .put("iterations", row.iterations).put("salt", row.saltB64).put("nonce", row.nonceB64).put("data", row.ciphertextB64)
    private fun parseRow(row: JSONObject) = CredentialEntity(row.getLong("id"), row.getString("owner"),
        if (row.isNull("remote")) null else row.getString("remote"), row.getString("algorithm"), row.getString("kdf"),
        row.getInt("iterations"), row.getString("salt"), row.getString("nonce"), row.getString("data"))

    @Synchronized fun login(email: String, password: CharArray): LocalSession {
        val record = find(normalizeEmail(email)) ?: throw IllegalArgumentException("Correo o contraseña incorrectos.")
        val bundle = try { unwrapPassword(record, password) }
        catch (_: Exception) { throw IllegalArgumentException("Correo o contraseña incorrectos.") }
        try {
            val secret = bundle.copyOfRange(0, 32)
            return try { session(record, secret) } finally { secret.fill(0) }
        }
        finally { bundle.fill(0) }
    }

    @Synchronized fun recover(email: String, code: String, newPassword: CharArray): LocalSession {
        requirePassword(newPassword)
        val record = find(normalizeEmail(email)) ?: throw IllegalArgumentException("El correo o la clave no son correctos.")
        val recovery = decodeRecovery(code)
        val secret = try { open(SecretKeySpec(recovery, "AES"), record.getJSONObject("recovery"), aad(record, "recovery")) }
        catch (_: Exception) { recovery.fill(0); throw IllegalArgumentException("El correo o la clave no son correctos.") }
        try {
            require(secret.size == 32)
            wrapPassword(record, newPassword, secret, recovery)
            write(record)
            return session(record, secret)
        } finally { secret.fill(0); recovery.fill(0) }
    }

    private fun wrapPassword(record: JSONObject, password: CharArray, secret: ByteArray, recovery: ByteArray) {
        val salt = bytes(16)
        val key = derive(password, salt)
        val bundle = secret + recovery
        try {
            record.put("salt", encode(salt)).put("password", seal(key, bundle, aad(record, "password")))
        } finally { bundle.fill(0); key.encoded.fill(0) }
    }

    private fun unwrapPassword(record: JSONObject, password: CharArray): ByteArray {
        require(password.size in 1..256)
        val salt = decode(record.getString("salt"))
        require(salt.size == 16)
        val key = derive(password, salt)
        return try {
            open(key, record.getJSONObject("password"), aad(record, "password")).also { require(it.size == 64) }
        } finally { key.encoded.fill(0) }
    }

    private fun session(record: JSONObject, secret: ByteArray): LocalSession =
        LocalSession(record.getString("id"), encode(secret).toCharArray())

    private fun find(email: String): JSONObject? = files().map { read(it) }.firstOrNull { it.getString("email") == email }
    private fun files(): List<File> = directory.listFiles { f -> f.name.endsWith(".bin") || f.name.endsWith(".bin.bak") }
        ?.map { if (it.name.endsWith(".bak")) File(it.path.removeSuffix(".bak")) else it }?.distinct().orEmpty()
    private fun file(id: String): File {
        require(UUID.fromString(id).toString() == id)
        return File(directory, "$id.bin")
    }

    private fun read(file: File): JSONObject {
        val atomic = AtomicFile(file)
        val bytes = atomic.openRead().use { input ->
            val out = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            var count = input.read(buffer)
            while (count != -1) {
                require(out.size() + count <= 16 * 1024 * 1024) { "Archivo de cuenta inválido." }
                out.write(buffer, 0, count); count = input.read(buffer)
            }
            out.toByteArray()
        }
        val plain = open(deviceKey(), JSONObject(String(bytes, Charsets.UTF_8)), file.name.toByteArray())
        return try {
            JSONObject(String(plain, Charsets.UTF_8)).also {
                require(it.getInt("version") == 1 && file(it.getString("id")).name == file.name)
            }
        } finally { plain.fill(0) }
    }

    private fun write(record: JSONObject) {
        check(directory.isDirectory || directory.mkdirs()) { "No se pudo guardar la cuenta." }
        val target = file(record.getString("id"))
        val plain = record.toString().toByteArray()
        val encrypted = try { seal(deviceKey(), plain, target.name.toByteArray()).toString().toByteArray() }
        finally { plain.fill(0) }
        require(encrypted.size <= 16 * 1024 * 1024) { "La bóveda anterior es demasiado grande para importarla de una vez." }
        val atomic = AtomicFile(target)
        val output = atomic.startWrite()
        try { output.write(encrypted); atomic.finishWrite(output) }
        catch (error: Exception) { atomic.failWrite(output); throw error }
    }

    private fun deviceKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
        }.generateKey()
    }

    private fun derive(password: CharArray, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password, salt, 600_000, 256)
        val derived = try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
        return try { SecretKeySpec(derived, "AES") } finally { derived.fill(0) }
    }
    private fun seal(key: SecretKey, plain: ByteArray, aad: ByteArray): JSONObject {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(aad)
        return JSONObject().put("iv", encode(cipher.iv)).put("data", encode(cipher.doFinal(plain)))
    }
    private fun open(key: SecretKey, sealed: JSONObject, aad: ByteArray): ByteArray {
        val iv = decode(sealed.getString("iv"))
        require(iv.size == 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        cipher.updateAAD(aad)
        return cipher.doFinal(decode(sealed.getString("data")))
    }
    private fun aad(record: JSONObject, purpose: String) =
        "thevault:1:${record.getString("id")}:${record.getString("email")}:$purpose".toByteArray()
    private fun bytes(size: Int) = ByteArray(size).also(random::nextBytes)
    private fun encode(value: ByteArray) = Base64.encodeToString(value, Base64.NO_WRAP)
    private fun decode(value: String) = Base64.decode(value, Base64.NO_WRAP)

    companion object {
        fun normalizeEmail(value: String): String = value.trim().lowercase(Locale.ROOT).also {
            require(it.length <= 254 && Patterns.EMAIL_ADDRESS.matcher(it).matches()) { "Escribe un correo válido." }
        }
        fun requirePassword(password: CharArray) {
            require(password.size in 12..256 && password.any { !it.isWhitespace() }) {
                "Usa una contraseña de 12 a 256 caracteres."
            }
        }
        private fun decodeRecovery(value: String): ByteArray {
            val normalized = value.filterNot { it.isWhitespace() || it == '-' }.uppercase(Locale.ROOT)
            require(normalized.length == 64 && normalized.all { it in "0123456789ABCDEF" }) {
                "Escribe la clave de recuperación completa."
            }
            return ByteArray(32) { normalized.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
        }
    }
}
