package com.example.thevault

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.thevault.ui.auth.AuthViewModel
import com.example.thevault.ui.screens.LoginScreen
import com.example.thevault.ui.screens.SplashScreen
import com.example.thevault.ui.screens.VaultAppScreen
import com.example.thevault.ui.theme.TheVaultTheme
import com.example.thevault.ui.vault.VaultViewModel
import com.example.thevault.ui.auth.AuthStage
import com.example.thevault.ui.transfer.BluetoothTransferViewModel
import com.example.thevault.ui.screens.BluetoothTransferScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent { TheVaultApp() }
    }
}

@Composable
internal fun TheVaultApp(
    authViewModel: AuthViewModel = viewModel(),
    vaultViewModel: VaultViewModel = viewModel(),
    transferViewModel: BluetoothTransferViewModel = viewModel()
) {
    val authState = authViewModel.uiState
    val vaultState = vaultViewModel.uiState
    val activity = LocalContext.current as? ComponentActivity

    LaunchedEffect(activity, vaultState.darkTheme) {
        val style = if (vaultState.darkTheme) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        activity?.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    LaunchedEffect(authState.isAuthenticated) {
        if (authState.isAuthenticated && !vaultState.isUnlocked && !vaultState.isUnlocking) {
            val masterPassword = authViewModel.consumeVaultPassword()
            if (masterPassword == null) {
                authViewModel.rejectVaultUnlock("Vuelve a ingresar tu contraseña maestra.")
            } else {
                vaultViewModel.unlock(masterPassword, ownerId = authState.accountId, onFailure = authViewModel::rejectVaultUnlock)
            }
        }
    }

    TheVaultTheme(darkTheme = vaultState.darkTheme) {
        when {
            transferViewModel.uiState.open -> BluetoothTransferScreen(transferViewModel) { email ->
                if (email != null) {
                    authViewModel.changeStage(AuthStage.LOGIN)
                    authViewModel.onEmailChange(email)
                }
            }
            authState.showSplash -> SplashScreen()
            !authState.isAuthenticated -> LoginScreen(
                state = authState,
                onEmailChange = authViewModel::onEmailChange,
                onPasswordChange = authViewModel::onPasswordChange,
                onTogglePassword = authViewModel::togglePasswordVisibility,
                onLogin = authViewModel::login,
                onConfirmationChange = authViewModel::onConfirmationChange,
                onRecoveryChange = authViewModel::onRecoveryChange,
                onKeyNoted = authViewModel::onKeyNoted,
                onStageChange = authViewModel::changeStage,
                onOldPasswordChange = authViewModel::onOldPasswordChange,
                onImportChange = authViewModel::onImportChange,
                onBack = authViewModel::back,
                onReceiveTransfer = transferViewModel::openReceiver
            )
            !vaultState.isUnlocked -> SplashScreen()
            else -> VaultAppScreen(
                viewModel = vaultViewModel,
                onTransfer = {
                    authState.accountId?.let { transferViewModel.openSender(it, authState.email) }
                },
                onLogout = {
                    vaultViewModel.clearSession()
                    authViewModel.logout()
                }
            )
        }
    }
}
