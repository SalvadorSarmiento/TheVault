package com.example.thevault.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.thevault.ui.auth.AuthStage
import com.example.thevault.ui.auth.AuthUiState
import com.example.thevault.ui.components.VaultLogo
import com.example.thevault.ui.components.VaultPrimaryButton

@Composable
fun SplashScreen() {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        VaultLogo(Modifier.size(132.dp))
        Spacer(Modifier.height(24.dp))
        Text("TheVault", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text("Tus credenciales, en un solo lugar", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private data class AccessPage(val title: String, val description: String, val action: String)

private fun page(stage: AuthStage) = when (stage) {
    AuthStage.WELCOME -> AccessPage("Tus cuentas,\nen un solo lugar.",
        "Guarda tus contraseñas y encuentra lo que necesitas sin tener que recordarlo todo.", "Conocer TheVault")
    AuthStage.PRIVACY -> AccessPage("Un espacio\nsolo para ti.",
        "Tu contraseña maestra abre tu bóveda. Tus credenciales quedan cifradas en este dispositivo, incluso sin internet.", "Continuar")
    AuthStage.KEY_EXPLANATION -> AccessPage("Una llave extra,\npor si la necesitas.",
        "Al crear tu cuenta recibirás una clave de recuperación. Anótala fuera del teléfono: te ayudará si olvidas tu contraseña.", "Crear mi cuenta")
    AuthStage.LOGIN -> AccessPage("Qué bueno verte.", "Entra con el acceso que creaste en este dispositivo.", "Iniciar sesión")
    AuthStage.REGISTER -> AccessPage("Empecemos por ti.", "¿Qué correo quieres usar para identificar tu cuenta?", "Continuar")
    AuthStage.REGISTER_PASSWORD -> AccessPage("Tu contraseña,\ntu primera llave.",
        "Elige una frase de al menos 12 caracteres que puedas recordar.", "Continuar")
    AuthStage.IMPORT_LEGACY -> AccessPage("Tu bóveda anterior.",
        "Encontramos credenciales guardadas en esta app. Puedes conservarlas con su contraseña anterior.", "Continuar")
    AuthStage.RECOVERY_KEY -> AccessPage("Anota tu clave.",
        "Anota los 8 grupos de izquierda a derecha, fila por fila. Guárdalos en un lugar privado, fuera del teléfono.", "Ya la anoté")
    AuthStage.CONFIRM_KEY -> AccessPage("Un último paso.",
        "Comprueba tu copia antes de guardar la cuenta. Puedes volver atrás para revisar la clave.", "Guardar cuenta y entrar")
    AuthStage.RECOVER -> AccessPage("Recupera tu acceso.",
        "Necesitas el correo de tu cuenta y la clave completa que anotaste.", "Continuar")
    AuthStage.RESET_PASSWORD -> AccessPage("Elige una nueva llave.",
        "Crea tu nueva contraseña. Comprobaremos la clave al confirmar y conservaremos tus credenciales.", "Cambiar contraseña y entrar")
}

@Composable
fun LoginScreen(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onLogin: () -> Unit,
    onConfirmationChange: (String) -> Unit = {},
    onRecoveryChange: (String) -> Unit = {},
    onKeyNoted: (Boolean) -> Unit = {},
    onStageChange: (AuthStage) -> Unit = {},
    onOldPasswordChange: (String) -> Unit = {},
    onImportChange: (Boolean) -> Unit = {},
    onBack: () -> Unit = {},
    onReceiveTransfer: () -> Unit = {}
) {
    val intro = state.stage in setOf(AuthStage.WELCOME, AuthStage.PRIVACY, AuthStage.KEY_EXPLANATION)
    val canGoBack = state.stage !in setOf(AuthStage.WELCOME, AuthStage.LOGIN)
    val copy = page(state.stage)
    BackHandler(enabled = canGoBack || state.isLoading) { if (!state.isLoading) onBack() }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (canGoBack) IconButton(onClick = onBack, enabled = !state.isLoading) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver al paso anterior")
            }
            VaultLogo(Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Text("TheVault", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(24.dp))
        AccessProgress(state.stage)
        Spacer(Modifier.height(24.dp))

        key(state.stage) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                if (intro) IntroIllustration(when (state.stage) {
                    AuthStage.WELCOME -> Icons.Outlined.Folder
                    AuthStage.PRIVACY -> Icons.Outlined.Lock
                    else -> Icons.Outlined.Key
                })
                Text(copy.title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(copy.description, style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                when (state.stage) {
                    AuthStage.WELCOME -> AccessNote("Un lugar para tus cuentas personales, de trabajo y de estudio.")
                    AuthStage.PRIVACY -> AccessNote("Una contraseña maestra para entrar. Las demás, dentro de tu bóveda.")
                    AuthStage.KEY_EXPLANATION -> RecoveryLimit()
                    AuthStage.REGISTER, AuthStage.LOGIN, AuthStage.RECOVER -> {
                        AccessField(state.email, onEmailChange, "Correo electrónico", state,
                            keyboardType = KeyboardType.Email)
                        when (state.stage) {
                            AuthStage.REGISTER -> AccessNote("El correo identifica tu cuenta local. No enviamos mensajes ni verificamos el correo.")
                            AuthStage.LOGIN -> PasswordField(state, onPasswordChange, onTogglePassword)
                            else -> {
                                AccessField(state.recoveryInput, onRecoveryChange, "Clave de recuperación completa", state, secret = true)
                                RecoveryLimit()
                            }
                        }
                    }
                    AuthStage.REGISTER_PASSWORD, AuthStage.RESET_PASSWORD -> {
                        PasswordField(state, onPasswordChange, onTogglePassword)
                        AccessField(state.confirmation, onConfirmationChange, "Confirma la contraseña", state, secret = true)
                    }
                    AuthStage.IMPORT_LEGACY -> {
                        Row(Modifier.fillMaxWidth().toggleable(value = state.importLegacy, role = Role.Checkbox,
                            enabled = !state.isLoading, onValueChange = onImportChange), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = state.importLegacy, onCheckedChange = null)
                            Text("Conservar mi bóveda anterior", modifier = Modifier.weight(1f).padding(12.dp))
                        }
                        if (state.importLegacy) AccessField(state.oldPassword, onOldPasswordChange,
                            "Contraseña de la bóveda anterior", state, secret = true)
                        else AccessNote("La bóveda anterior seguirá guardada. Esta cuenta empezará vacía.")
                    }
                    AuthStage.RECOVERY_KEY -> {
                        RecoveryCode(state.recoveryCode)
                        AccessNote("Aún no hemos guardado tu cuenta. Al confirmar, guardaremos también una copia cifrada de esta clave.")
                        RecoveryLimit()
                    }
                    AuthStage.CONFIRM_KEY -> {
                        AccessField(state.recoveryInput, onRecoveryChange, "Últimos 8 caracteres", state)
                        Row(Modifier.fillMaxWidth().toggleable(value = state.keyNoted, role = Role.Checkbox,
                            enabled = !state.isLoading, onValueChange = onKeyNoted), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = state.keyNoted, onCheckedChange = null)
                            Text("Guardé mi clave en un lugar seguro", modifier = Modifier.weight(1f).padding(12.dp))
                        }
                        AccessNote("Necesitarás tu contraseña o tu clave anotada para volver a abrir la bóveda.")
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp))
        }
        Spacer(Modifier.height(16.dp))
        VaultPrimaryButton(copy.action, onClick = onLogin, loading = state.isLoading)
        if (state.stage == AuthStage.LOGIN || state.stage == AuthStage.WELCOME) {
            TextButton(onClick = onReceiveTransfer, enabled = !state.isLoading,
                modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Recibir por Bluetooth") }
        }
        if (state.stage == AuthStage.LOGIN) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { onStageChange(AuthStage.WELCOME) }, enabled = !state.isLoading) { Text("Crear cuenta") }
                TextButton(onClick = { onStageChange(AuthStage.RECOVER) }, enabled = !state.isLoading) { Text("Olvidé mi contraseña") }
            }
        } else {
            TextButton(onClick = { onStageChange(AuthStage.LOGIN) }, enabled = !state.isLoading,
                modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(if (intro || state.stage in setOf(AuthStage.REGISTER, AuthStage.REGISTER_PASSWORD)) "Ya tengo una cuenta" else "Volver al inicio de sesión")
            }
        }
    }
}

@Composable
private fun AccessProgress(stage: AuthStage) {
    val (step, total, label) = when (stage) {
        AuthStage.WELCOME -> Triple(1, 3, "DESCUBRE THEVAULT")
        AuthStage.PRIVACY -> Triple(2, 3, "TU PRIVACIDAD")
        AuthStage.KEY_EXPLANATION -> Triple(3, 3, "TU RECUPERACIÓN")
        AuthStage.REGISTER -> Triple(1, 4, "TU CUENTA")
        AuthStage.REGISTER_PASSWORD, AuthStage.IMPORT_LEGACY -> Triple(2, 4, "TU CONTRASEÑA")
        AuthStage.RECOVERY_KEY -> Triple(3, 4, "TU CLAVE DE RECUPERACIÓN")
        AuthStage.CONFIRM_KEY -> Triple(4, 4, "CONFIRMA TU COPIA")
        AuthStage.RECOVER -> Triple(1, 2, "RECUPERA TU ACCESO")
        AuthStage.RESET_PASSWORD -> Triple(2, 2, "NUEVA CONTRASEÑA")
        AuthStage.LOGIN -> return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text("$step de $total", style = MaterialTheme.typography.labelMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(total) { index ->
                Box(Modifier.weight(1f).height(4.dp).background(
                    if (index < step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape))
            }
        }
    }
}

@Composable
private fun IntroIllustration(icon: ImageVector) {
    Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(176.dp).background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f), CircleShape),
            contentAlignment = Alignment.Center) {
            Box(Modifier.size(132.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(36.dp)),
                contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(64.dp))
            }
        }
    }
}

@Composable
private fun RecoveryCode(code: String) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("TU CLAVE PERSONAL", style = MaterialTheme.typography.labelMedium)
        code.split('-').chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { group ->
                    Text(group, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PasswordField(state: AuthUiState, onChange: (String) -> Unit, onToggle: () -> Unit) {
    OutlinedTextField(value = state.password, onValueChange = onChange,
        label = { Text(if (state.stage == AuthStage.RESET_PASSWORD) "Nueva contraseña maestra" else "Contraseña maestra") },
        trailingIcon = { IconButton(onClick = onToggle) {
            Icon(if (state.showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                contentDescription = if (state.showPassword) "Ocultar contraseña" else "Mostrar contraseña")
        } }, singleLine = true, enabled = !state.isLoading,
        visualTransformation = if (state.showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
}

@Composable
private fun AccessField(value: String, onChange: (String) -> Unit, label: String, state: AuthUiState,
    secret: Boolean = false, keyboardType: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) },
        singleLine = true, enabled = !state.isLoading, shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (secret) KeyboardType.Password else keyboardType))
}

@Composable
private fun AccessNote(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
            RoundedCornerShape(16.dp)).padding(16.dp))
}

@Composable
private fun RecoveryLimit() = AccessNote("La clave recupera el acceso en este dispositivo. No restaura datos borrados ni una app desinstalada.")
