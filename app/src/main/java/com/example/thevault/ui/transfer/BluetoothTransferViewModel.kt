package com.example.thevault.ui.transfer

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.thevault.data.transfer.BluetoothPeer
import com.example.thevault.data.transfer.BluetoothVaultTransport
import com.example.thevault.data.transfer.VaultBluetoothTransport
import com.example.thevault.data.transfer.VaultTransferService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class TransferStep { CHOOSE, READY, WAITING, RESTORE, DONE }

data class BluetoothTransferState(
    val open: Boolean = false,
    val sending: Boolean = false,
    val step: TransferStep = TransferStep.CHOOSE,
    val email: String = "",
    val peers: List<BluetoothPeer> = emptyList(),
    val selected: BluetoothPeer? = null,
    val password: String = "",
    val confirmation: String = "",
    val recovery: String = "",
    val busy: Boolean = false,
    val error: String? = null
) {
    override fun toString() = "BluetoothTransferState(step=$step, busy=$busy)"
}

class BluetoothTransferViewModel @JvmOverloads constructor(
    application: Application,
    private val transport: VaultBluetoothTransport = BluetoothVaultTransport(application),
    private val service: VaultTransferService = VaultTransferService(application)
) : AndroidViewModel(application) {
    var uiState by mutableStateOf(BluetoothTransferState())
        private set
    private var accountId = ""
    private var packet: ByteArray? = null
    private var job: Job? = null
    private var epoch = 0L

    fun openSender(id: String, email: String) { close(); accountId = id; uiState = BluetoothTransferState(open = true, sending = true, email = email) }
    fun openReceiver() { close(); uiState = BluetoothTransferState(open = true) }
    fun password(value: String) { if (!uiState.busy) uiState = uiState.copy(password = value.take(256), error = null) }
    fun confirmation(value: String) { if (!uiState.busy) uiState = uiState.copy(confirmation = value.take(256), error = null) }
    fun recovery(value: String) { if (!uiState.busy) uiState = uiState.copy(recovery = value.take(100), error = null) }
    fun select(peer: BluetoothPeer) { if (!uiState.busy) uiState = uiState.copy(selected = peer, error = null) }
    fun permissionDenied() { uiState = uiState.copy(error = "Sin el permiso de dispositivos cercanos no se puede transferir. Puedes activarlo en los ajustes de Android.") }

    fun refresh() {
        if (uiState.busy) return
        runCatching { transport.pairedDevices() }
            .onSuccess { peers -> uiState = uiState.copy(peers = peers, selected = null,
                error = if (peers.isEmpty()) "Vincula los dos teléfonos en los ajustes de Bluetooth y vuelve a actualizar." else null) }
            .onFailure { uiState = uiState.copy(error =
                if (it is IllegalStateException) it.message else "No se pudieron leer los teléfonos. Revisa el permiso de dispositivos cercanos.") }
    }

    fun next() { if (uiState.selected != null && !uiState.busy) uiState = uiState.copy(step = TransferStep.READY, error = null) }
    fun chooseAgain() { if (!uiState.busy) uiState = uiState.copy(step = TransferStep.CHOOSE, password = "", error = null) }

    fun transfer() {
        if (uiState.busy || uiState.step != TransferStep.READY) return
        val state = uiState
        val peer = state.selected ?: return
        if (state.sending && state.password.isBlank()) { uiState = state.copy(error = "Escribe tu contraseña maestra para autorizar el envío."); return }
        val version = epoch
        uiState = state.copy(busy = true, step = TransferStep.WAITING, password = "", error = null)
        job = viewModelScope.launch {
            try {
                if (state.sending) {
                    val password = state.password.toCharArray()
                    val copy = try { withContext(Dispatchers.IO) { service.export(accountId, state.email, password) } }
                    finally { password.fill('\u0000') }
                    try { transport.send(peer, copy) } finally { copy.fill(0) }
                    if (version == epoch) uiState = uiState.copy(step = TransferStep.DONE)
                } else {
                    val received = transport.receive(peer)
                    if (version == epoch) {
                        packet = received
                        uiState = uiState.copy(step = TransferStep.RESTORE)
                    } else received.fill(0)
                }
            } catch (_: TimeoutCancellationException) {
                if (version == epoch) uiState = uiState.copy(step = TransferStep.READY, error = "Se agotó el tiempo. Acerca los teléfonos y vuelve a intentarlo.")
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (version == epoch) uiState = uiState.copy(step = TransferStep.READY,
                    error = if (error is IllegalArgumentException || error is IllegalStateException) error.message
                    else "Se interrumpió la conexión. No se cambió la bóveda original. Vuelve a intentarlo.")
            } finally { if (version == epoch) uiState = uiState.copy(busy = false) }
        }
    }

    fun restore() {
        if (uiState.busy || uiState.step != TransferStep.RESTORE) return
        val received = packet ?: return
        val state = uiState
        if (state.password != state.confirmation) { uiState = state.copy(error = "Las contraseñas no coinciden."); return }
        val version = epoch
        uiState = state.copy(busy = true, error = null)
        job = viewModelScope.launch {
            val password = state.password.toCharArray()
            try {
                val email = withContext(Dispatchers.IO) { service.restore(received, state.recovery, password) }
                if (version == epoch) {
                    packet?.fill(0); packet = null
                    uiState = uiState.copy(step = TransferStep.DONE, email = email)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (version == epoch) uiState = uiState.copy(error =
                    if (error is IllegalArgumentException || error is IllegalStateException) error.message
                    else "No se pudo restaurar la copia. Comprueba la clave y vuelve a intentarlo.")
            } finally {
                password.fill('\u0000')
                if (version == epoch) uiState = uiState.copy(busy = false, password = "", confirmation = "", recovery = "")
            }
        }
    }

    fun close() {
        epoch++
        job?.cancel(); job = null
        transport.close()
        packet?.fill(0); packet = null
        accountId = ""
        uiState = BluetoothTransferState()
    }
    override fun onCleared() { close(); super.onCleared() }
}
