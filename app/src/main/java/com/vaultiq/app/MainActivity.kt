
package com.vaultiq.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vaultiq.app.core.navigation.VaultNavGraph
import com.vaultiq.app.core.navigation.VaultRoutes
import com.vaultiq.app.ui.theme.VaultGold
import com.vaultiq.app.ui.theme.VaultIQTheme

private data class VaultTab(
    val route: String,
    val label: String,
    val symbol: String
)

private val vaultTabs = listOf(
    VaultTab(VaultRoutes.DOWNLOAD, "Download", "↓"),
    VaultTab(VaultRoutes.PLAY, "Play", "▶"),
    VaultTab(VaultRoutes.SETTINGS, "Settings", "⚙")
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VaultIQTheme {
                val navController = rememberNavController()

                val backStackEntry by
                    navController.currentBackStackEntryAsState()

                val currentRoute =
                    backStackEntry?.destination?.route
                        ?: VaultRoutes.DOWNLOAD

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            vaultTabs.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentRoute == tab.route,
                                    onClick = {
                                        navController.navigate(tab.route) {
                                            popUpTo(
                                                navController.graph.startDestinationId
                                            ) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Text(
                                            text = tab.symbol,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                    },
                                    label = {
                                        Text(tab.label)
                                    },
                                    alwaysShowLabel = true
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    VaultNavGraph(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
