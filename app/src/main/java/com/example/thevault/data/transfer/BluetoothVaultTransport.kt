package com.example.thevault.data.transfer

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.io.Closeable
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class BluetoothPeer(val address: String, val name: String)

interface VaultBluetoothTransport {
    fun pairedDevices(): List<BluetoothPeer>
    suspend fun send(peer: BluetoothPeer, packet: ByteArray)
    suspend fun receive(peer: BluetoothPeer): ByteArray
    fun close()
}

/** Solo conexiones RFCOMM seguras a dispositivos vinculados elegidos por el usuario. */
class BluetoothVaultTransport(private val context: Context) : VaultBluetoothTransport {
    private val active = AtomicReference<Operation?>()

    @android.annotation.SuppressLint("MissingPermission")
    private fun adapter(): android.bluetooth.BluetoothAdapter {
        check(Build.VERSION.SDK_INT < 31 || ContextCompat.checkSelfPermission(context,
            Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            "Permite el acceso a dispositivos cercanos para usar Bluetooth."
        }
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        check(adapter != null) { "Este dispositivo no tiene Bluetooth disponible." }
        check(adapter.isEnabled) { "Activa Bluetooth en los ajustes de Android." }
        return adapter
    }

    @android.annotation.SuppressLint("MissingPermission")
    override fun pairedDevices(): List<BluetoothPeer> = adapter().bondedDevices
        .map { BluetoothPeer(it.address, it.name ?: "Teléfono vinculado") }.sortedBy { it.name }

    @android.annotation.SuppressLint("MissingPermission")
    override suspend fun send(peer: BluetoothPeer, packet: ByteArray) = runOperation { operation ->
        val adapter = adapter()
        check(adapter.bondedDevices.any { it.address == peer.address }) { "Vincula primero los teléfonos en Android." }
        val socket = operation.track(adapter.getRemoteDevice(peer.address).createRfcommSocketToServiceRecord(SERVICE))
        socket.connect()
        TransferWire.write(socket.outputStream, packet)
        TransferWire.awaitAcknowledgement(socket.inputStream)
    }

    @android.annotation.SuppressLint("MissingPermission")
    override suspend fun receive(peer: BluetoothPeer): ByteArray = runOperation { operation ->
        val adapter = adapter()
        check(adapter.bondedDevices.any { it.address == peer.address }) { "Vincula primero los teléfonos en Android." }
        val server = operation.track(adapter.listenUsingRfcommWithServiceRecord("TheVault", SERVICE))
        val socket = operation.track(server.accept())
        server.close()
        check(socket.remoteDevice.address == peer.address) { "La conexión no viene del teléfono que elegiste." }
        val packet = TransferWire.read(socket.inputStream)
        TransferWire.acknowledge(socket.outputStream)
        packet
    }

    override fun close() { active.get()?.close() }

    private suspend fun <T> runOperation(block: (Operation) -> T): T = withTimeout(120_000) {
        suspendCancellableCoroutine { continuation ->
            val operation = Operation()
            if (!active.compareAndSet(null, operation)) {
                continuation.resumeWithException(IllegalStateException("Ya hay una transferencia en curso."))
                return@suspendCancellableCoroutine
            }
            continuation.invokeOnCancellation { operation.close() }
            thread(name = "TheVault-Bluetooth", isDaemon = true) {
                try { continuation.resume(block(operation)) }
                catch (error: Exception) { continuation.resumeWithException(error) }
                finally { operation.close(); active.compareAndSet(operation, null) }
            }
        }
    }

    private class Operation : Closeable {
        private val resources = mutableListOf<Closeable>()
        private var closed = false
        @Synchronized fun <T : Closeable> track(resource: T): T {
            if (closed) { resource.close(); error("Transferencia cancelada.") }
            resources.add(resource)
            return resource
        }
        @Synchronized override fun close() {
            closed = true
            resources.asReversed().forEach { runCatching { it.close() } }
            resources.clear()
        }
    }

    private companion object {
        val SERVICE: UUID = UUID.fromString("a2067821-9278-48a5-b9cf-208779da082f")
    }
}
