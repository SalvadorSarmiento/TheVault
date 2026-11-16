package com.example.thevault.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.thevault.ui.theme.VaultHeroText
import com.example.thevault.ui.theme.VaultHeroMuted
import com.example.thevault.ui.theme.VaultHeroAccent
import com.example.thevault.ui.components.VaultHero
import com.example.thevault.data.model.Credential
import com.example.thevault.ui.components.CategoryIcon
import com.example.thevault.ui.components.VaultLogo
import com.example.thevault.ui.vault.VaultFilter
import com.example.thevault.ui.vault.VaultUiState

@Composable
fun VaultListScreen(
    state: VaultUiState,
    credentials: List<Credential>,
    onQueryChange: (String) -> Unit,
    onFilterChange: (VaultFilter) -> Unit,
    onCredentialClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.padding(horizontal = 24.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 24.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            VaultHero {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("TU ESPACIO PERSONAL", style = MaterialTheme.typography.labelSmall, color = VaultHeroMuted)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = state.categoryFilter?.label ?: "Mi bóveda",
                            style = MaterialTheme.typography.headlineLarge,
                            color = VaultHeroText
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Todo a mano. Solo para ti.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VaultHeroMuted
                        )
                    }
                    Box(
                        Modifier.size(48.dp).background(VaultHeroText.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = VaultHeroAccent)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    Column {
                        Text("${state.credentials.size}", style = MaterialTheme.typography.headlineMedium, color = VaultHeroText)
                        Text("Cuentas guardadas", style = MaterialTheme.typography.labelMedium, color = VaultHeroMuted)
                    }
                    Column {
                        Text("${state.credentials.count { it.isFavorite }}", style = MaterialTheme.typography.headlineMedium, color = VaultHeroAccent)
                        Text("Favoritas", style = MaterialTheme.typography.labelMedium, color = VaultHeroMuted)
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar credencial") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp)
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VaultFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.categoryFilter == null && state.filter == filter,
                        onClick = { onFilterChange(filter) },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }
        item {
            Text(
                if (state.query.isBlank()) "Tus cuentas" else "Resultados",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        if (credentials.isEmpty()) {
            item {
                EmptyVaultState(hasSearch = state.query.isNotBlank() || state.categoryFilter != null)
            }
        } else {
            items(credentials, key = { it.id }) { credential ->
                CredentialRow(credential = credential, onClick = { onCredentialClick(credential.id) })
            }
        }
    }
}

@Composable
private fun CredentialRow(credential: Credential, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CategoryIcon(credential.category)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    credential.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    credential.username,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (credential.isFavorite) {
                Icon(Icons.Outlined.Star, contentDescription = "Favorita", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyVaultState(hasSearch: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (hasSearch) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text("Sin coincidencias", style = MaterialTheme.typography.titleLarge)
            Text(
                "Prueba con otro nombre o revisa la escritura.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            VaultLogo(Modifier.size(88.dp))
            Spacer(Modifier.height(20.dp))
            Text("Tu primera credencial", style = MaterialTheme.typography.titleLarge)
            Text(
                "Pulsa + para agregar una cuenta.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
