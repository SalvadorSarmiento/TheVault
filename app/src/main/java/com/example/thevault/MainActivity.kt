package com.example.thevault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.thevault.ui.theme.TheVaultTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TheVaultTheme {
                TheVaultApp()
            }
        }
    }
}

@Composable
fun TheVaultApp() {
    var showSplash by rememberSaveable { mutableStateOf(true) }
    var isLoggedIn by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(2000)
        showSplash = false
    }

    when {
        showSplash -> SplashScreen()
        isLoggedIn -> VaultScreen(onLogout = { isLoggedIn = false })
        else -> LoginScreen(onLogin = { isLoggedIn = true })
    }
}

@Composable
fun SplashScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.thevaul_icon),
            contentDescription = "Icono de TheVault",
            modifier = Modifier.size(180.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "TheVault",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@Composable
fun LoginScreen(onLogin: () -> Unit) {
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.thevaul_icon),
            contentDescription = "Icono de TheVault",
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Bienvenido a TheVault",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Ingresa cualquier contraseña para continuar",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Contraseña") },
            placeholder = { Text("Ingresa una contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                password = ""
                onLogin()
            },
            enabled = password.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Ingresar")
        }
    }
}

private enum class VaultSection(val title: String) {
    HOME("Inicio"),
    PASSWORDS("Credenciales"),
    SETTINGS("Configuración")
}

@Composable
fun VaultScreen(onLogout: () -> Unit) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedSection by rememberSaveable { mutableStateOf(VaultSection.HOME) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "TheVault",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(24.dp)
                )

                VaultSection.entries.forEach { section ->
                    NavigationDrawerItem(
                        label = { Text(section.title) },
                        selected = selectedSection == section,
                        onClick = {
                            selectedSection = section
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                NavigationDrawerItem(
                    label = { Text("Cerrar sesión") },
                    selected = false,
                    onClick = onLogout,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { scope.launch { drawerState.open() } }) {
                    Text("Menú")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = selectedSection.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            VaultSectionContent(
                section = selectedSection,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            )
        }
    }
}

@Composable
private fun VaultSectionContent(section: VaultSection, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val title: String
        val message: String

        when (section) {
            VaultSection.HOME -> {
                title = "Bóveda desbloqueada"
                message = "Ya puedes entrar a TheVault."
            }

            VaultSection.PASSWORDS -> {
                title = "Tus credenciales"
                message = "Todavía no hay credenciales guardadas."
            }

            VaultSection.SETTINGS -> {
                title = "Configuración"
                message = "Aquí aparecerán las opciones de la aplicación."
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
