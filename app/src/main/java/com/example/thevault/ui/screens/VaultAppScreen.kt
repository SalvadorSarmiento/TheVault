package com.example.thevault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.thevault.ui.vault.VaultDestination
import com.example.thevault.ui.vault.VaultViewModel

private data class NavigationItem(
    val destination: VaultDestination,
    val label: String,
    val icon: ImageVector
)

private val navigationItems = listOf(
    NavigationItem(VaultDestination.VAULT, "Bóveda", Icons.Outlined.VpnKey),
    NavigationItem(VaultDestination.GENERATOR, "Generador", Icons.Outlined.Edit),
    NavigationItem(VaultDestination.CATEGORIES, "Categorías", Icons.Outlined.BusinessCenter),
    NavigationItem(VaultDestination.SETTINGS, "Ajustes", Icons.Outlined.Settings)
)

@Composable
fun VaultAppScreen(viewModel: VaultViewModel, onLogout: () -> Unit) {
    val state = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }
    val isTopLevel = state.destination in navigationItems.map { it.destination }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (isTopLevel) {
                NavigationBar {
                    navigationItems.forEach { item ->
                        NavigationBarItem(
                            selected = state.destination == item.destination,
                            onClick = { viewModel.navigate(item.destination) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { androidx.compose.material3.Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (state.destination == VaultDestination.VAULT) {
                FloatingActionButton(onClick = viewModel::startCreate) {
                    Icon(Icons.Outlined.Add, contentDescription = "Añadir credencial")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize()
                .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                .padding(if (isTopLevel) padding else PaddingValues(bottom = padding.calculateBottomPadding()))
                .statusBarsPadding()
        ) {
            when (state.destination) {
                VaultDestination.VAULT -> VaultListScreen(
                    state = state,
                    credentials = viewModel.visibleCredentials,
                    onQueryChange = viewModel::onQueryChange,
                    onFilterChange = viewModel::setFilter,
                    onCredentialClick = viewModel::openCredential
                )
                VaultDestination.GENERATOR -> PasswordGeneratorScreen(state, viewModel)
                VaultDestination.CATEGORIES -> CategoriesScreen(
                    credentials = state.credentials,
                    onCategoryClick = viewModel::openCategory
                )
                VaultDestination.SETTINGS -> SettingsScreen(
                    darkTheme = state.darkTheme,
                    onDarkThemeChange = viewModel::setDarkTheme,
                    onLogout = onLogout
                )
                VaultDestination.DETAIL -> CredentialDetailScreen(
                    credential = viewModel.selectedCredential,
                    showDeleteDialog = state.showDeleteDialog,
                    onBack = viewModel::onBack,
                    onEdit = viewModel::startEdit,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onRequestDelete = viewModel::requestDelete,
                    onDismissDelete = viewModel::dismissDelete,
                    onConfirmDelete = viewModel::deleteSelected,
                    onMessage = viewModel::showMessage
                )
                VaultDestination.EDITOR -> CredentialEditorScreen(
                    draft = state.draft,
                    onBack = viewModel::onBack,
                    onDraftChange = viewModel::updateDraft,
                    onSave = viewModel::saveDraft
                )
            }
        }
    }
}
