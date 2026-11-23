package com.example.thevault.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.thevault.data.model.Credential
import com.example.thevault.data.model.CredentialCategory
import com.example.thevault.ui.components.*
import com.example.thevault.ui.vault.CredentialDraft

@Composable
fun CredentialDetailScreen(
    credential: Credential?, showDeleteDialog: Boolean, onBack: () -> Unit, onEdit: () -> Unit,
    onToggleFavorite: () -> Unit, onRequestDelete: () -> Unit, onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit, onMessage: (String) -> Unit
) {
    if (credential == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TextButton(onClick = onBack) { Text("Volver a la bóveda") }
        }
        return
    }
    val clipboard = LocalClipboardManager.current
    var showPassword by rememberSaveable(credential.id) { mutableStateOf(false) }
    fun copy(value: String, message: String) { clipboard.setText(AnnotatedString(value)); onMessage(message) }

    LazyColumn(Modifier.fillMaxSize().navigationBarsPadding(), contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item { ScreenHeader("Detalle de la cuenta", onBack = onBack) }
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CategoryIcon(credential.category)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(credential.name, style = MaterialTheme.typography.titleLarge)
                        Text(credential.category.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onToggleFavorite) {
                        Icon(if (credential.isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (credential.isFavorite) "Quitar de favoritos" else "Agregar a favoritos")
                    }
                }
            }
        }
        item { SectionHeading("Datos de acceso", "Consulta o copia cada dato por separado.") }
        item {
            DetailCard("Usuario o correo", credential.username) {
                DetailAction("Copiar usuario", Icons.Outlined.ContentCopy) { copy(credential.username, "Usuario copiado") }
            }
        }
        item {
            DetailCard("Contraseña", if (showPassword) credential.password else "•".repeat(12)) {
                DetailAction(if (showPassword) "Ocultar" else "Mostrar",
                    if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    if (showPassword) "Ocultar contraseña" else "Mostrar contraseña") { showPassword = !showPassword }
                DetailAction("Copiar", Icons.Outlined.ContentCopy, "Copiar contraseña") { copy(credential.password, "Contraseña copiada") }
            }
        }
        if (credential.website.isNotBlank() || credential.notes.isNotBlank()) {
            item { SectionHeading("Información adicional") }
        }
        if (credential.website.isNotBlank()) item {
            DetailCard("Sitio web", credential.website) {
                DetailAction("Copiar enlace", Icons.Outlined.ContentCopy) { copy(credential.website, "Enlace copiado") }
            }
        }
        if (credential.notes.isNotBlank()) item { DetailCard("Notas", credential.notes) }
        item {
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                VaultPrimaryButton("Editar credencial", onClick = onEdit)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                TextButton(onClick = onRequestDelete, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Eliminar credencial", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 12.dp))
                }
            }
        }
    }
    if (showDeleteDialog) AlertDialog(onDismissRequest = onDismissDelete,
        title = { Text("¿Eliminar credencial?") },
        text = { Text("${credential.name} se eliminará de la bóveda. Esta acción no se puede deshacer.") },
        confirmButton = { TextButton(onClick = onConfirmDelete) { Text("Eliminar", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismissDelete) { Text("Cancelar") } })
}

@Composable
private fun DetailCard(label: String, value: String, actions: (@Composable () -> Unit)? = null) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            actions?.let {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) { it() }
            }
        }
    }
}

@Composable
private fun DetailAction(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String? = null, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(20.dp))
        Text(text, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
fun CredentialEditorScreen(draft: CredentialDraft, onBack: () -> Unit,
    onDraftChange: ((CredentialDraft) -> CredentialDraft) -> Unit, onSave: () -> Unit,
    isSaving: Boolean = false, showSaveBar: Boolean = true) {
    var categoryMenuOpen by remember { mutableStateOf(false) }
    val isEditing = draft.id != null
    Column(Modifier.fillMaxSize().navigationBarsPadding().imePadding()) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)) {
            item { ScreenHeader(if (isEditing) "Editar credencial" else "Nueva credencial",
                "Completa un bloque a la vez.", onBack = onBack) }
            item {
                VaultSection("La cuenta", "Un nombre para reconocerla fácilmente.") {
                    OutlinedTextField(value = draft.name,
                        onValueChange = { value -> onDraftChange { it.copy(name = value) } },
                        modifier = Modifier.fillMaxWidth(), label = { Text("Nombre") },
                        placeholder = { Text("Nombre del servicio") }, singleLine = true, enabled = !isSaving,
                        shape = RoundedCornerShape(16.dp), isError = draft.submitted && draft.name.isBlank(),
                        supportingText = if (draft.submitted && draft.name.isBlank()) {{ Text("El nombre es obligatorio.") }} else null)
                }
            }
            item {
                VaultSection("Datos de acceso", "El usuario y la contraseña de este servicio.") {
                    OutlinedTextField(value = draft.username,
                        onValueChange = { value -> onDraftChange { it.copy(username = value) } },
                        modifier = Modifier.fillMaxWidth(), label = { Text("Usuario o correo") }, singleLine = true,
                        enabled = !isSaving, shape = RoundedCornerShape(16.dp),
                        isError = draft.submitted && draft.username.isBlank(),
                        supportingText = if (draft.submitted && draft.username.isBlank()) {{ Text("Ingresa un usuario o correo.") }} else null)
                    OutlinedTextField(value = draft.password,
                        onValueChange = { value -> onDraftChange { it.copy(password = value) } },
                        modifier = Modifier.fillMaxWidth(), label = { Text("Contraseña") }, singleLine = true,
                        enabled = !isSaving, shape = RoundedCornerShape(16.dp),
                        visualTransformation = if (draft.showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = { IconButton(onClick = { onDraftChange { it.copy(showPassword = !it.showPassword) } }, enabled = !isSaving) {
                            Icon(if (draft.showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (draft.showPassword) "Ocultar contraseña" else "Mostrar contraseña")
                        } }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                        isError = draft.submitted && draft.password.isBlank(),
                        supportingText = if (draft.submitted && draft.password.isBlank()) {{ Text("Ingresa una contraseña.") }} else null)
                    if (draft.password.isNotBlank()) PasswordStrength(draft.password)
                }
            }
            item {
                VaultSection("Organización y notas", "La categoría ayuda a encontrar esta cuenta después.") {
                    Box {
                        OutlinedButton(onClick = { categoryMenuOpen = true }, enabled = !isSaving,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(16.dp)) {
                            Text("Categoría: ${draft.category.label}", modifier = Modifier.weight(1f))
                            Icon(Icons.Outlined.ExpandMore, contentDescription = "Cambiar categoría")
                        }
                        DropdownMenu(expanded = categoryMenuOpen, onDismissRequest = { categoryMenuOpen = false }) {
                            CredentialCategory.entries.forEach { category ->
                                DropdownMenuItem(text = { Text(category.label) }, onClick = {
                                    onDraftChange { it.copy(category = category) }; categoryMenuOpen = false
                                })
                            }
                        }
                    }
                    OutlinedTextField(value = draft.website,
                        onValueChange = { value -> onDraftChange { it.copy(website = value) } },
                        modifier = Modifier.fillMaxWidth(), label = { Text("Sitio web (opcional)") },
                        singleLine = true, enabled = !isSaving, shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
                    OutlinedTextField(value = draft.notes,
                        onValueChange = { value -> onDraftChange { it.copy(notes = value) } },
                        modifier = Modifier.fillMaxWidth(), minLines = 4, label = { Text("Notas (opcional)") },
                        enabled = !isSaving, shape = RoundedCornerShape(16.dp))
                }
            }
        }
        if (showSaveBar) CredentialSaveBar(draft, onSave, isSaving)
    }
}

@Composable
internal fun CredentialSaveBar(draft: CredentialDraft, onSave: () -> Unit, isSaving: Boolean,
    modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
            VaultPrimaryButton(if (draft.id != null) "Guardar cambios" else "Guardar credencial",
                onClick = onSave, loading = isSaving)
        }
    }
}
