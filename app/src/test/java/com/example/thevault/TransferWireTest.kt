package com.example.thevault

import com.example.thevault.data.transfer.TransferEnvelope
import com.example.thevault.data.transfer.TransferWire
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.EOFException
import javax.crypto.AEADBadTagException
import org.junit.Assert.*
import org.junit.Test

class TransferWireTest {
    @Test fun cifraElPaqueteYExigeLaClaveCorrecta() {
        val key = ByteArray(32) { it.toByte() }
        val plain = "cuenta y contraseña de prueba".toByteArray()
        val packet = TransferEnvelope.encrypt(plain, key)
        assertFalse(String(packet).contains("contraseña"))
        assertArrayEquals(plain, TransferEnvelope.decrypt(packet, key))
        assertThrows(AEADBadTagException::class.java) { TransferEnvelope.decrypt(packet, ByteArray(32)) }
        packet[packet.lastIndex] = (packet.last().toInt() xor 1).toByte()
        assertThrows(AEADBadTagException::class.java) { TransferEnvelope.decrypt(packet, key) }
    }

    @Test fun cadaEnvioUsaUnNonceNuevo() {
        val key = ByteArray(32)
        assertFalse(TransferEnvelope.encrypt(byteArrayOf(1), key)
            .contentEquals(TransferEnvelope.encrypt(byteArrayOf(1), key)))
    }

    @Test fun transfiereUnaTramaCompletaYConfirmaRecepcion() {
        val packet = byteArrayOf(1, 2, 3)
        val bytes = ByteArrayOutputStream()
        TransferWire.write(bytes, packet)
        assertArrayEquals(packet, TransferWire.read(ByteArrayInputStream(bytes.toByteArray())))
        val ack = ByteArrayOutputStream()
        TransferWire.acknowledge(ack)
        TransferWire.awaitAcknowledgement(ByteArrayInputStream(ack.toByteArray()))
    }

    @Test fun rechazaCortesYLongitudesFueraDeLimite() {
        val bytes = ByteArrayOutputStream()
        TransferWire.write(bytes, byteArrayOf(1, 2, 3))
        assertThrows(EOFException::class.java) {
            TransferWire.read(ByteArrayInputStream(bytes.toByteArray().dropLast(1).toByteArray()))
        }
        for (size in listOf(-1, 0, TransferEnvelope.MAX_BYTES + 1)) {
            val invalid = ByteArrayOutputStream()
            DataOutputStream(invalid).apply { writeInt(0x54564231); writeInt(size) }
            assertThrows(IllegalArgumentException::class.java) { TransferWire.read(ByteArrayInputStream(invalid.toByteArray())) }
        }
        assertThrows(IllegalArgumentException::class.java) { TransferWire.read(ByteArrayInputStream(ByteArray(8))) }
    }
}
