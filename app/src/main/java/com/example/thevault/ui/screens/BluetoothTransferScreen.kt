package com.example.thevault.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.thevault.ui.transfer.BluetoothTransferViewModel
import com.example.thevault.ui.transfer.TransferStep

@Composable
fun BluetoothTransferScreen(viewModel: BluetoothTransferViewModel, onFinished: (String?) -> Unit) {
    val state = viewModel.uiState
    val context = LocalContext.current
    val importing = state.busy && state.step == TransferStep.RESTORE
    val scrollState = rememberScrollState()
    LaunchedEffect(state.step) { scrollState.scrollTo(0) }
    val close = { viewModel.close() }
    BackHandler { if (!importing) close() }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) viewModel.refresh() else viewModel.permissionDenied()
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .statusBarsPadding().navigationBarsPadding().imePadding()
        .padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Icon(Icons.Outlined.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            TextButton(onClick = close, enabled = !importing) { Text(if (state.busy) "Cancelar" else "Cerrar") }
        }
        Column(Modifier.weight(1f).verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text("TRANSFERENCIA LOCAL · BETA", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary)
            Text(when (state.step) {
                TransferStep.CHOOSE -> "Elige el otro teléfono."
                TransferStep.READY -> if (state.sending) "Autoriza tu copia." else "Todo listo para recibir."
                TransferStep.WAITING -> if (state.sending) "Enviando tu bóveda…" else "Esperando tu copia…"
                TransferStep.RESTORE -> "Recupera tu bóveda."
                TransferStep.DONE -> if (state.sending) "Copia enviada." else "Tu cuenta está lista."
            }, style = MaterialTheme.typography.headlineLarge)

            when (state.step) {
                TransferStep.CHOOSE -> {
                    TransferNote("1 · Vincula los dos teléfonos en los ajustes de Bluetooth de Android.\n\n2 · Abre TheVault en ambos y elige el otro dispositivo.")
                    Text("Solo transfiere entre teléfonos tuyos. Necesitas el teléfono anterior y la clave de recuperación para restaurar.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
                        modifier = Modifier.fillMaxWidth()) { Text("Abrir ajustes de Bluetooth") }
                    OutlinedButton(onClick = {
                        if (Build.VERSION.SDK_INT >= 31 && ContextCompat.checkSelfPermission(context,
                            Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            permission.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        } else viewModel.refresh()
                    }, modifier = Modifier.fillMaxWidth()) { Text("Actualizar teléfonos vinculados") }
                    state.peers.forEach { peer ->
                        OutlinedCard(onClick = { viewModel.select(peer) }, modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(if (state.selected == peer) 2.dp else 1.dp,
                                if (state.selected == peer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(peer.name, style = MaterialTheme.typography.titleMedium)
                                Text(peer.address, style = MaterialTheme.typography.bodySmall)
                                if (state.selected == peer) Text("Seleccionado", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                TransferStep.READY -> {
                    TransferNote("Teléfono elegido: ${state.selected?.name.orEmpty()}")
                    if (state.sending) {
                        Text("En el otro teléfono, pulsa primero Esperar copia. Luego escribe aquí tu contraseña maestra para enviar esta cuenta y sus credenciales.")
                        TransferField(state.password, viewModel::password, "Contraseña maestra")
                        Text("La cuenta original se conserva. Tu contraseña y tu clave de recuperación no se envían.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("Pulsa Esperar copia. Después, en el teléfono anterior, entra a Ajustes → Transferir por Bluetooth y envía la bóveda.")
                        TransferNote("Mantén ambas apps abiertas y los teléfonos cerca. La espera termina a los dos minutos.")
                    }
                    TextButton(onClick = viewModel::chooseAgain) { Text("Elegir otro teléfono") }
                }
                TransferStep.WAITING -> {
                    CircularProgressIndicator()
                    Text("Mantén TheVault abierto en ambos teléfonos. Puedes cancelar sin borrar la cuenta original.")
                }
                TransferStep.RESTORE -> {
                    TransferNote("Copia recibida. Aún no se ha creado ni reemplazado ninguna cuenta.")
                    Text("Escribe la clave que anotaste al crear la cuenta original y elige una nueva contraseña para este teléfono.")
                    TransferField(state.recovery, viewModel::recovery, "Clave de recuperación completa", !state.busy)
                    TransferField(state.password, viewModel::password, "Nueva contraseña (mínimo 12 caracteres)", !state.busy)
                    TransferField(state.confirmation, viewModel::confirmation, "Repite la nueva contraseña", !state.busy)
                    Text("Conserva tu clave de recuperación: seguirá siendo la misma. Una cuenta existente no se sobrescribe.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TransferStep.DONE -> TransferNote(if (state.sending)
                    "El otro teléfono recibió la copia cifrada. Allí falta introducir la clave de recuperación para restaurarla. Tu bóveda original sigue aquí."
                    else "Se restauraron tu cuenta y sus credenciales. Inicia sesión con la nueva contraseña. Los cambios futuros no se sincronizan automáticamente.")
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(16.dp))
        when (state.step) {
            TransferStep.CHOOSE -> Button(onClick = viewModel::next, enabled = state.selected != null,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Continuar") }
            TransferStep.READY -> Button(onClick = viewModel::transfer,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(if (state.sending) "Enviar copia cifrada" else "Esperar copia")
            }
            TransferStep.RESTORE -> Button(onClick = viewModel::restore, enabled = !state.busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(if (state.busy) "Restaurando…" else "Restaurar cuenta")
            }
            TransferStep.DONE -> Button(onClick = { onFinished(state.email.takeIf { !state.sending }); viewModel.close() },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(if (state.sending) "Volver a mi bóveda" else "Ir a iniciar sesión")
            }
            TransferStep.WAITING -> Unit
        }
    }
}

@Composable
private fun TransferNote(text: String) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()) { Text(text, modifier = Modifier.padding(20.dp)) }
}

@Composable
private fun TransferField(value: String, onChange: (String) -> Unit, label: String, enabled: Boolean = true) {
    OutlinedTextField(value, onChange, label = { Text(label) }, enabled = enabled, singleLine = true,
        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
}
