package com.example.thevault.data.transfer

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** El paquete completo, incluida la identidad de la cuenta, viaja cifrado. */
object TransferEnvelope {
    const val MAX_BYTES = 8 * 1024 * 1024
    private val header = "TheVault:Bluetooth:1".toByteArray(Charsets.US_ASCII)

    fun encrypt(plain: ByteArray, recovery: ByteArray): ByteArray {
        require(recovery.size == 32 && plain.size <= MAX_BYTES - header.size - 28)
        val nonce = ByteArray(12).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(recovery, "AES"), GCMParameterSpec(128, nonce))
        cipher.updateAAD(header)
        return header + nonce + cipher.doFinal(plain)
    }

    fun decrypt(packet: ByteArray, recovery: ByteArray): ByteArray {
        require(recovery.size == 32 && packet.size in (header.size + 28)..MAX_BYTES)
        require(packet.copyOfRange(0, header.size).contentEquals(header)) { "Copia no compatible." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(recovery, "AES"),
            GCMParameterSpec(128, packet.copyOfRange(header.size, header.size + 12)))
        cipher.updateAAD(header)
        return cipher.doFinal(packet, header.size + 12, packet.size - header.size - 12)
    }
}

/** Tramas acotadas: una conexión cortada nunca produce una copia parcial válida. */
object TransferWire {
    private const val MAGIC = 0x54564231
    private const val ACK = 0x54564F4B

    fun write(output: OutputStream, packet: ByteArray) {
        require(packet.size in 1..TransferEnvelope.MAX_BYTES)
        DataOutputStream(output).apply { writeInt(MAGIC); writeInt(packet.size); write(packet); flush() }
    }

    fun read(input: InputStream): ByteArray = DataInputStream(input).run {
        require(readInt() == MAGIC) { "Conexión no compatible con TheVault." }
        val size = readInt()
        require(size in 1..TransferEnvelope.MAX_BYTES) { "La copia supera el límite permitido." }
        ByteArray(size).also(::readFully)
    }

    fun acknowledge(output: OutputStream) = DataOutputStream(output).run { writeInt(ACK); flush() }
    fun awaitAcknowledgement(input: InputStream) {
        check(DataInputStream(input).readInt() == ACK) { "El otro teléfono no confirmó la recepción." }
    }
}
