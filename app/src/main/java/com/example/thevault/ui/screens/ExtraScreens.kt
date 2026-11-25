package com.example.thevault.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import com.example.thevault.ui.components.VaultHero
import com.example.thevault.ui.components.VaultSection
import com.example.thevault.ui.components.SectionHeading
import androidx.compose.ui.unit.dp
import com.example.thevault.data.model.Credential
import com.example.thevault.data.model.CredentialCategory
import com.example.thevault.ui.components.CategoryIcon
import com.example.thevault.ui.components.InfoCard
import com.example.thevault.ui.components.ScreenHeader
import com.example.thevault.ui.components.VaultLogo
import com.example.thevault.ui.components.VaultPrimaryButton
import com.example.thevault.ui.vault.VaultUiState
import com.example.thevault.ui.vault.VaultViewModel

@Composable
fun PasswordGeneratorScreen(state: VaultUiState, viewModel: VaultViewModel) {
    val clipboard = LocalClipboardManager.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        item { ScreenHeader("Generador", "Personaliza una contraseña nueva.") }
        item {
            VaultHero {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text("TU NUEVA CONTRASEÑA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(state.generatedPassword, style = MaterialTheme.typography.headlineSmall,
                        fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface)
                    androidx.compose.material3.TextButton(onClick = {
                        clipboard.setText(AnnotatedString(state.generatedPassword))
                        viewModel.showMessage("Contraseña copiada")
                    }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Copiar contraseña", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        }

        item {
            VaultSection("Longitud", "Elige entre 8 y 32 caracteres.") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Caracteres", style = MaterialTheme.typography.bodyLarge)
                    Text("${state.generatorLength}", fontWeight = FontWeight.Bold)
                }
                Slider(value = state.generatorLength.toFloat(),
                    onValueChange = { viewModel.setGeneratorLength(it.toInt()) }, valueRange = 8f..32f, steps = 23)
            }
        }
        item {
            VaultSection("Qué incluir", "Mantén activa al menos una opción.") {
                GeneratorSwitch("Mayúsculas (A–Z)", state.includeUppercase) { viewModel.setGeneratorOptions(uppercase = it) }
                GeneratorSwitch("Minúsculas (a–z)", state.includeLowercase) { viewModel.setGeneratorOptions(lowercase = it) }
                GeneratorSwitch("Números (0–9)", state.includeNumbers) { viewModel.setGeneratorOptions(numbers = it) }
                GeneratorSwitch("Símbolos (!@#$)", state.includeSymbols) { viewModel.setGeneratorOptions(symbols = it) }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                VaultPrimaryButton("Usar contraseña", onClick = viewModel::useGeneratedPassword)
                OutlinedButton(onClick = viewModel::regeneratePassword,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(16.dp)) {
                    Text("Generar otra")
                }
            }
        }
    }
}

@Composable
private fun GeneratorSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
fun CategoriesScreen(credentials: List<Credential>, onCategoryClick: (CredentialCategory) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { ScreenHeader("Categorías", "Organiza tus cuentas por tipo de uso.") }
        item { SectionHeading("Elige una categoría") }
        CredentialCategory.entries.forEach { category ->
            val count = credentials.count { it.category == category }
            item(category.name) {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onCategoryClick(category) },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        CategoryIcon(category)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(category.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "$count ${if (count == 1) "credencial" else "credenciales"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Outlined.ChevronRight, contentDescription = "Ver ${category.label}")
                    }
                }
            }
        }
        item {
            InfoCard(
                icon = Icons.Outlined.Info,
                title = "Cada cuenta en su lugar",
                body = "Elige una categoría al guardar una cuenta para encontrarla más rápido."
            )
        }
    }
}

@Composable
fun SettingsScreen(darkTheme: Boolean, onDarkThemeChange: (Boolean) -> Unit, onLogout: () -> Unit,
    onTransfer: () -> Unit = {}) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 24.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { ScreenHeader("Ajustes") }
        item {
            VaultSection("Tu espacio personal") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    VaultLogo(Modifier.height(56.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("TheVault", style = MaterialTheme.typography.titleLarge)
                        Text("Tus cuentas, en este dispositivo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item { SectionHeading("Apariencia") }
        item {
            SettingsRow(icon = Icons.Outlined.DarkMode, title = "Tema oscuro",
                subtitle = if (darkTheme) "Activado" else "Desactivado",
                trailing = { Switch(checked = darkTheme, onCheckedChange = onDarkThemeChange) })
        }
        item { SectionHeading("Seguridad y almacenamiento") }
        item {
            VaultSection("Cambia de teléfono") {
                Text("Envía una copia cifrada a otro teléfono cercano. Necesitarás ambos dispositivos y tu clave de recuperación.")
                OutlinedButton(onClick = onTransfer, modifier = Modifier.fillMaxWidth()) {
                    Text("Transferir por Bluetooth")
                }
            }
        }
        item {
            InfoCard(
                icon = Icons.Outlined.Lock,
                title = "Bloqueo automático",
                body = "Por ahora, usa Cerrar sesión cuando termines. El bloqueo por inactividad aún no está disponible."
            )
        }
        item {
            InfoCard(
                icon = Icons.Outlined.Storage,
                title = "Almacenamiento local",
                body = "Tus cuentas se guardan cifradas en este dispositivo. El acceso y la recuperación funcionan sin conexión."
            )
        }
        item {
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Outlined.Logout, contentDescription = null)
                Text("Cerrar sesión", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            trailing()
        }
    }
}
