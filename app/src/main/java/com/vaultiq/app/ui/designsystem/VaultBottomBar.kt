
package com.vaultiq.app.ui.designsystem

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.vaultiq.app.core.navigation.VaultRoutes
import com.vaultiq.app.ui.theme.VaultGold
import com.vaultiq.app.ui.theme.VaultMuted
import com.vaultiq.app.ui.theme.VaultNavy
import com.vaultiq.app.ui.theme.VaultPanel

data class VaultNavigationItem(
    val route: String,
    val label: String,
    val symbol: String
)

private val navigationItems = listOf(
    VaultNavigationItem(VaultRoutes.DOWNLOAD, "Download", "↓"),
    VaultNavigationItem(VaultRoutes.PLAY, "Play", "▶"),
    VaultNavigationItem(VaultRoutes.SETTINGS, "Settings", "⚙")
)

@Composable
fun VaultBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = VaultPanel,
        contentColor = VaultGold
    ) {
        navigationItems.forEach { item ->
            val selected = currentRoute == item.route

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        onNavigate(item.route)
                    }
                },
                icon = {
                    Text(
                        text = item.symbol,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        fontWeight = if (selected) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        }
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = VaultGold,
                    selectedTextColor = VaultGold,
                    unselectedIconColor = VaultMuted,
                    unselectedTextColor = VaultMuted,
                    indicatorColor = VaultNavy
                ),
                alwaysShowLabel = true
            )
        }
    }
}
