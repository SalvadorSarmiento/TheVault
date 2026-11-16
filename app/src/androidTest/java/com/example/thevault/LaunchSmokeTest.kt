package com.example.thevault

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LaunchSmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchReachesLogin() {
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText("Inicia sesión").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Inicia sesión").assertIsDisplayed()
        compose.onNodeWithText("Correo electrónico").assertIsDisplayed()
    }

    @Test
    fun demoLoginUnlocksLocalVault() {
        assumeFalse("Requires a configured Firebase test account", BuildConfig.FIREBASE_CONFIGURED)
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText("Inicia sesión").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Correo electrónico").performTextInput("ana@ejemplo.com")
        compose.onNodeWithText("Contraseña maestra").performTextInput("123456")
        compose.onNodeWithText("Iniciar sesión").performScrollTo().performClick()
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithText("Mi bóveda").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Mi bóveda").assertIsDisplayed()
    }
}
