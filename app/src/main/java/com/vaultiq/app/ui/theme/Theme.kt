
package com.vaultiq.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val VaultColorScheme = darkColorScheme(
    primary = VaultGold,
    onPrimary = VaultNavy,
    secondary = VaultSilver,
    background = VaultNavy,
    onBackground = VaultWhite,
    surface = VaultPanel,
    onSurface = VaultWhite,
    surfaceVariant = VaultPanelLight,
    onSurfaceVariant = VaultSilver,
    outline = VaultBorder
)

@Composable
fun VaultIQTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VaultColorScheme,
        content = content
    )
}
