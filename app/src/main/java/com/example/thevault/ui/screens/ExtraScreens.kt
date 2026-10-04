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
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { ScreenHeader("Generador", "Personaliza una contraseña nueva.") }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Contraseña generada", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(10.dp))
                        Text(state.generatedPassword, style = MaterialTheme.typography.titleLarge)
                    }
                    IconButton(onClick = {
                        clipboard.setText(AnnotatedString(state.generatedPassword))
                        viewModel.showMessage("Contraseña copiada")
                    }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copiar contraseña")
                    }
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Longitud", style = MaterialTheme.typography.titleMedium)
                Text("${state.generatorLength}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = state.generatorLength.toFloat(),
                onValueChange = { viewModel.setGeneratorLength(it.toInt()) },
                valueRange = 8f..32f,
                steps = 23
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("8", style = MaterialTheme.typography.bodyMedium)
                Text("32", style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            GeneratorSwitch("Mayúsculas (A–Z)", state.includeUppercase) {
                viewModel.setGeneratorOptions(uppercase = it)
            }
            GeneratorSwitch("Minúsculas (a–z)", state.includeLowercase) {
                viewModel.setGeneratorOptions(lowercase = it)
            }
            GeneratorSwitch("Números (0–9)", state.includeNumbers) {
                viewModel.setGeneratorOptions(numbers = it)
            }
            GeneratorSwitch("Símbolos (!@#$)", state.includeSymbols) {
                viewModel.setGeneratorOptions(symbols = it)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = viewModel::regeneratePassword,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Generar otra") }
                VaultPrimaryButton(
                    text = "Usar contraseña",
                    onClick = viewModel::useGeneratedPassword,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun GeneratorSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun CategoriesScreen(credentials: List<Credential>, onCategoryClick: (CredentialCategory) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenHeader("Categorías", "Organiza tus cuentas por tipo de uso.") }
        item { Spacer(Modifier.height(10.dp)) }
        CredentialCategory.entries.forEach { category ->
            val count = credentials.count { it.category == category }
            item(category.name) {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onCategoryClick(category) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CategoryIcon(category)
                        Column(Modifier.weight(1f)) {
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
                title = "Datos de demostración",
                body = "Las categorías y credenciales se reinician al cerrar la aplicación."
            )
        }
    }
}

@Composable
fun SettingsScreen(darkTheme: Boolean, onDarkThemeChange: (Boolean) -> Unit, onLogout: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 24.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ScreenHeader("Ajustes") }
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                VaultLogo(Modifier.padding(6.dp).height(58.dp))
                Column(Modifier.padding(start = 14.dp)) {
                    Text("TheVault", style = MaterialTheme.typography.titleLarge)
                    Text("ana@ejemplo.com", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { Text("Apariencia", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
        item {
            SettingsRow(
                icon = Icons.Outlined.DarkMode,
                title = "Tema oscuro",
                subtitle = if (darkTheme) "Activado" else "Desactivado",
                trailing = { Switch(checked = darkTheme, onCheckedChange = onDarkThemeChange) }
            )
        }
        item { Text("Seguridad", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
        item {
            InfoCard(
                icon = Icons.Outlined.Lock,
                title = "Bloqueo automático",
                body = "En el CRUD real esta opción bloqueará la bóveda después de 1 minuto de inactividad."
            )
        }
        item {
            InfoCard(
                icon = Icons.Outlined.Storage,
                title = "Almacenamiento local",
                body = "Esta versión usa un repositorio en memoria para separar las vistas de la futura persistencia."
            )
        }
        item {
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(56.dp),
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
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            trailing()
        }
    }
}
