package com.example.thevault.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.thevault.data.model.Credential
import com.example.thevault.data.model.CredentialCategory
import com.example.thevault.ui.components.CategoryIcon
import com.example.thevault.ui.components.PasswordStrength
import com.example.thevault.ui.components.ScreenHeader
import com.example.thevault.ui.components.VaultPrimaryButton
import com.example.thevault.ui.vault.CredentialDraft

@Composable
fun CredentialDetailScreen(
    credential: Credential?,
    showDeleteDialog: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRequestDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onMessage: (String) -> Unit
) {
    if (credential == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TextButton(onClick = onBack) { Text("Volver a la bóveda") }
        }
        return
    }

    val clipboard = LocalClipboardManager.current
    var showPassword by rememberSaveable(credential.id) { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeader(
                title = "Credencial",
                onBack = onBack,
                action = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Editar credencial")
                    }
                }
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                CategoryIcon(credential.category)
                Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    Text(credential.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        credential.category.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (credential.isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (credential.isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        item {
            DetailCard(
                label = "Usuario o correo",
                value = credential.username,
                actionIcon = Icons.Outlined.ContentCopy,
                actionDescription = "Copiar usuario",
                onAction = {
                    clipboard.setText(AnnotatedString(credential.username))
                    onMessage("Usuario copiado")
                }
            )
        }
        item {
            DetailCard(
                label = "Contraseña",
                value = if (showPassword) credential.password else "•".repeat(12),
                actionIcon = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                actionDescription = if (showPassword) "Ocultar contraseña" else "Mostrar contraseña",
                secondaryAction = {
                    IconButton(onClick = {
                        clipboard.setText(AnnotatedString(credential.password))
                        onMessage("Contraseña copiada")
                    }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copiar contraseña")
                    }
                },
                onAction = { showPassword = !showPassword }
            )
        }
        if (credential.website.isNotBlank()) {
            item {
                DetailCard(
                    label = "Sitio web",
                    value = credential.website,
                    actionIcon = Icons.Outlined.Language,
                    actionDescription = "Abrir sitio web",
                    onAction = { onMessage("Enlace de demostración") }
                )
            }
        }
        if (credential.notes.isNotBlank()) {
            item { DetailCard(label = "Notas", value = credential.notes) }
        }
        item {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null)
                Text("Editar credencial", modifier = Modifier.padding(start = 8.dp))
            }
        }
        item {
            TextButton(onClick = onRequestDelete, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text("Eliminar credencial", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text("¿Eliminar credencial?") },
            text = { Text("${credential.name} se eliminará de esta demostración. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = onConfirmDelete) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = onDismissDelete) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun DetailCard(
    label: String,
    value: String,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    actionDescription: String = "",
    secondaryAction: (@Composable () -> Unit)? = null,
    onAction: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }
            if (actionIcon != null) {
                IconButton(onClick = onAction) { Icon(actionIcon, contentDescription = actionDescription) }
            }
            secondaryAction?.invoke()
        }
    }
}

@Composable
fun CredentialEditorScreen(
    draft: CredentialDraft,
    onBack: () -> Unit,
    onDraftChange: ((CredentialDraft) -> CredentialDraft) -> Unit,
    onSave: () -> Unit
) {
    var categoryMenuOpen by remember { mutableStateOf(false) }
    val isEditing = draft.id != null

    LazyColumn(
        modifier = Modifier.fillMaxSize().navigationBarsPadding().imePadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ScreenHeader(
                title = if (isEditing) "Editar credencial" else "Nueva credencial",
                subtitle = "Datos de acceso del servicio",
                onBack = onBack
            )
        }
        item {
            OutlinedTextField(
                value = draft.name,
                onValueChange = { value -> onDraftChange { it.copy(name = value) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nombre") },
                placeholder = { Text("Nombre del servicio") },
                singleLine = true,
                isError = draft.submitted && draft.name.isBlank(),
                supportingText = if (draft.submitted && draft.name.isBlank()) {{ Text("El nombre es obligatorio.") }} else null
            )
        }
        item {
            OutlinedTextField(
                value = draft.username,
                onValueChange = { value -> onDraftChange { it.copy(username = value) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Usuario o correo") },
                singleLine = true,
                isError = draft.submitted && draft.username.isBlank(),
                supportingText = if (draft.submitted && draft.username.isBlank()) {{ Text("Ingresa un usuario o correo.") }} else null
            )
        }
        item {
            OutlinedTextField(
                value = draft.password,
                onValueChange = { value -> onDraftChange { it.copy(password = value) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Contraseña") },
                visualTransformation = if (draft.showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { onDraftChange { it.copy(showPassword = !it.showPassword) } }) {
                        Icon(
                            if (draft.showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (draft.showPassword) "Ocultar contraseña" else "Mostrar contraseña"
                        )
                    }
                },
                singleLine = true,
                isError = draft.submitted && draft.password.isBlank(),
                supportingText = if (draft.submitted && draft.password.isBlank()) {{ Text("Ingresa una contraseña.") }} else null,
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false)
            )
        }
        if (draft.password.isNotBlank()) item { PasswordStrength(draft.password) }
        item {
            OutlinedTextField(
                value = draft.website,
                onValueChange = { value -> onDraftChange { it.copy(website = value) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Sitio web (opcional)") },
                singleLine = true
            )
        }
        item {
            Box {
                OutlinedButton(
                    onClick = { categoryMenuOpen = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Categoría: ${draft.category.label}", modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.ExpandMore, contentDescription = "Cambiar categoría")
                }
                DropdownMenu(expanded = categoryMenuOpen, onDismissRequest = { categoryMenuOpen = false }) {
                    CredentialCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.label) },
                            onClick = {
                                onDraftChange { it.copy(category = category) }
                                categoryMenuOpen = false
                            }
                        )
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = draft.notes,
                onValueChange = { value -> onDraftChange { it.copy(notes = value) } },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                label = { Text("Notas (opcional)") }
            )
        }
        item {
            Spacer(Modifier.height(8.dp))
            VaultPrimaryButton(
                text = if (isEditing) "Guardar cambios" else "Guardar credencial",
                onClick = onSave
            )
        }
    }
}
