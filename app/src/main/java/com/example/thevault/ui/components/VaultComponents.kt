package com.example.thevault.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.ColumnScope
import com.example.thevault.ui.theme.VaultHeroStart
import com.example.thevault.ui.theme.VaultHeroEnd
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.thevault.R
import com.example.thevault.data.model.CredentialCategory

@Composable
fun VaultLogo(modifier: Modifier = Modifier, contentDescription: String = "Logo de TheVault") {
    Icon(
        painter = painterResource(R.drawable.ic_vault_logo),
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

@Composable
fun VaultPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                }
            }
            Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            action?.invoke()
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = if (onBack == null) 0.dp else 48.dp, top = 8.dp)
            )
        }
    }
}

fun categoryIcon(category: CredentialCategory): ImageVector = when (category) {
    CredentialCategory.PERSONAL -> Icons.Outlined.Email
    CredentialCategory.WORK -> Icons.Outlined.BusinessCenter
    CredentialCategory.STUDIES -> Icons.Outlined.School
    CredentialCategory.SHOPPING -> Icons.Outlined.ShoppingBag
}

@Composable
fun CategoryIcon(category: CredentialCategory, modifier: Modifier = Modifier) {
    val accent = when (category) {
        CredentialCategory.PERSONAL -> MaterialTheme.colorScheme.primary
        CredentialCategory.WORK -> MaterialTheme.colorScheme.secondary
        CredentialCategory.STUDIES -> MaterialTheme.colorScheme.tertiary
        CredentialCategory.SHOPPING -> MaterialTheme.colorScheme.primary
    }
    Box(
        modifier = modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = categoryIcon(category),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun VaultHero(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(VaultHeroStart, VaultHeroEnd)))
    ) {
        Box(
            Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp)
                .size(112.dp).clip(RoundedCornerShape(56.dp)).background(Color.White.copy(alpha = 0.05f))
        )
        Column(Modifier.fillMaxWidth().padding(24.dp), content = content)
    }
}

@Composable
fun PasswordStrength(password: String, modifier: Modifier = Modifier) {
    val score = listOf(
        password.length >= 8,
        password.any(Char::isUpperCase) && password.any(Char::isLowerCase),
        password.any(Char::isDigit),
        password.any { !it.isLetterOrDigit() }
    ).count { it }
    val label = when (score) {
        0, 1 -> "Débil"
        2, 3 -> "Media"
        else -> "Fuerte"
    }
    val color = if (score >= 3) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Fortaleza estimada", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(label, style = MaterialTheme.typography.labelLarge, color = color)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) { index ->
                Box(
                    Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(4.dp))
                        .background(if (index < score) color else MaterialTheme.colorScheme.outline)
                )
            }
        }
    }
}

@Composable
fun InfoCard(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SectionHeading(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() })
        subtitle?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun VaultSection(title: String, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            SectionHeading(title, subtitle)
            content()
        }
    }
}
