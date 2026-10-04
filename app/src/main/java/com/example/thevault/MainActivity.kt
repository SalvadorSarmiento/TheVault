package com.example.thevault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.thevault.ui.auth.AuthViewModel
import com.example.thevault.ui.screens.LoginScreen
import com.example.thevault.ui.screens.SplashScreen
import com.example.thevault.ui.screens.VaultAppScreen
import com.example.thevault.ui.theme.TheVaultTheme
import com.example.thevault.ui.vault.VaultViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TheVaultApp() }
    }
}

@Composable
private fun TheVaultApp(
    authViewModel: AuthViewModel = viewModel(),
    vaultViewModel: VaultViewModel = viewModel()
) {
    val authState = authViewModel.uiState
    val vaultState = vaultViewModel.uiState

    TheVaultTheme(darkTheme = vaultState.darkTheme) {
        when {
            authState.showSplash -> SplashScreen()
            !authState.isAuthenticated -> LoginScreen(
                state = authState,
                onEmailChange = authViewModel::onEmailChange,
                onPasswordChange = authViewModel::onPasswordChange,
                onTogglePassword = authViewModel::togglePasswordVisibility,
                onLogin = authViewModel::login
            )
            else -> VaultAppScreen(
                viewModel = vaultViewModel,
                onLogout = authViewModel::logout
            )
        }
    }
}
