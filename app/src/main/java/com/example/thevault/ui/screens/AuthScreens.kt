package com.example.thevault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.thevault.ui.theme.VaultHeroText
import com.example.thevault.ui.theme.VaultHeroMuted
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.example.thevault.R
import com.example.thevault.ui.auth.AuthStage
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.ui.text.font.FontFamily
import com.example.thevault.ui.components.VaultHero
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
        Text(
            "Tus credenciales, en un solo lugar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
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
    onImportChange: (Boolean) -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VaultHero {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(VaultHeroText.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_vault_logo), contentDescription = null, tint = VaultHeroText, modifier = Modifier.size(28.dp))
                }
                Text("TheVault", style = MaterialTheme.typography.titleLarge, color = VaultHeroText)
            }
            Spacer(Modifier.height(24.dp))
            Text("Tu mundo digital,\nbien protegido.", style = MaterialTheme.typography.headlineMedium, color = VaultHeroText)
            Spacer(Modifier.height(12.dp))
            Text("Tus cuentas. Tu espacio. Tu tranquilidad.", style = MaterialTheme.typography.bodyMedium, color = VaultHeroMuted)
        }
        Spacer(Modifier.height(28.dp))
        val registering = state.stage == AuthStage.REGISTER
        val recovering = state.stage == AuthStage.RECOVER
        val showingKey = state.stage == AuthStage.RECOVERY_KEY
        Text(when (state.stage) {
            AuthStage.LOGIN -> "Inicia sesión"
            AuthStage.REGISTER -> "Crea tu cuenta"
            AuthStage.RECOVERY_KEY -> "Anota tu clave"
            AuthStage.RECOVER -> "Recupera tu acceso"
        }, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text(if (showingKey) "Esta clave permite recuperar tu bóveda. Guárdala en un lugar privado, fuera del teléfono."
            else "Tu cuenta funciona en este dispositivo, sin conexión.")
        if (registering) Text("El correo identifica tu cuenta local; no enviamos ni verificamos correos.",
            style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(20.dp))
        if (showingKey) {
            Text(state.recoveryCode.replace("-", "\n"), fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer,
                    RoundedCornerShape(16.dp)).padding(20.dp))
            Spacer(Modifier.height(12.dp))
            Text("Aún no hemos guardado tu cuenta. Al confirmar, también guardaremos una copia cifrada con AES-256.")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = state.keyNoted, onCheckedChange = onKeyNoted, enabled = !state.isLoading)
                Text("Ya anoté mi clave de recuperación", modifier = Modifier.weight(1f))
            }
            OutlinedTextField(value = state.recoveryInput, onValueChange = onRecoveryChange,
                label = { Text("Últimos 8 caracteres") }, singleLine = true,
                enabled = !state.isLoading, modifier = Modifier.fillMaxWidth())
        } else {
            OutlinedTextField(value = state.email, onValueChange = onEmailChange,
                modifier = Modifier.fillMaxWidth(), label = { Text("Correo electrónico") },
                enabled = !state.isLoading, singleLine = true, shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next))
            Spacer(Modifier.height(14.dp))
            if (recovering) {
                OutlinedTextField(value = state.recoveryInput, onValueChange = onRecoveryChange,
                    label = { Text("Clave de recuperación completa") }, enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                Spacer(Modifier.height(14.dp))
            }
            OutlinedTextField(value = state.password, onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (recovering) "Nueva contraseña maestra" else "Contraseña maestra") },
                trailingIcon = { IconButton(onClick = onTogglePassword) {
                    Icon(if (state.showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (state.showPassword) "Ocultar contraseña" else "Mostrar contraseña")
                } },
                visualTransformation = if (state.showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true, enabled = !state.isLoading, shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
            if (registering || recovering) {
                Spacer(Modifier.height(12.dp))
                Text("Usa al menos 12 caracteres. Una frase larga es más fácil de recordar.")
                OutlinedTextField(value = state.confirmation, onValueChange = onConfirmationChange,
                    label = { Text("Confirma la contraseña") }, singleLine = true, enabled = !state.isLoading,
                    visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
            }
            if (registering && state.hasLegacyVault) {
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = state.importLegacy, onCheckedChange = onImportChange, enabled = !state.isLoading)
                    Text("Conservar mi bóveda anterior", modifier = Modifier.weight(1f))
                }
                if (state.importLegacy) OutlinedTextField(value = state.oldPassword, onValueChange = onOldPasswordChange,
                    label = { Text("Contraseña de la bóveda anterior") }, singleLine = true,
                    enabled = !state.isLoading, visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                else Text("La bóveda anterior seguirá guardada; esta cuenta empezará vacía.")
            }
        }
        state.error?.let { Spacer(Modifier.height(12.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(22.dp))
        VaultPrimaryButton(text = when (state.stage) {
            AuthStage.LOGIN -> "Iniciar sesión"
            AuthStage.REGISTER -> "Ver mi clave de recuperación"
            AuthStage.RECOVERY_KEY -> "Guardar cuenta y entrar"
            AuthStage.RECOVER -> "Cambiar contraseña y entrar"
        }, onClick = onLogin, loading = state.isLoading)
        if (state.stage == AuthStage.LOGIN) {
            TextButton(onClick = { onStageChange(AuthStage.REGISTER) }, enabled = !state.isLoading) { Text("Crear cuenta local") }
            TextButton(onClick = { onStageChange(AuthStage.RECOVER) }, enabled = !state.isLoading) { Text("Olvidé mi contraseña") }
        } else {
            TextButton(onClick = { onStageChange(AuthStage.LOGIN) }, enabled = !state.isLoading) {
                Text(if (showingKey) "Cancelar registro" else "Ya tengo una cuenta")
            }
        }
        if (showingKey || recovering) {
            Text("La recuperación necesita los datos de esta app en este dispositivo. No recupera una app desinstalada ni datos borrados.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
