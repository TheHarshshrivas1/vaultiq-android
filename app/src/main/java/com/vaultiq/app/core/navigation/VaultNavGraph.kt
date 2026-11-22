
package com.vaultiq.app.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

object VaultRoutes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}

@Composable
fun VaultNavGraph(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = VaultRoutes.HOME,
        modifier = modifier
    ) {
        composable(VaultRoutes.HOME) {
            PlaceholderScreen("VAULTIQ HOME")
        }

        composable(VaultRoutes.LIBRARY) {
            PlaceholderScreen("YOUR LIBRARY")
        }

        composable(VaultRoutes.HISTORY) {
            PlaceholderScreen("DOWNLOAD HISTORY")
        }

        composable(VaultRoutes.SETTINGS) {
            PlaceholderScreen("SETTINGS")
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
