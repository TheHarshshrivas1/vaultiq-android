
package com.vaultiq.app.core.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vaultiq.app.library.StorageAccessManager
import com.vaultiq.app.library.StorageSettingsSection

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
            SettingsScreen()
        }
    }
}

@Composable
private fun SettingsScreen() {
    val context = LocalContext.current

    val storageAccessManager = remember(context) {
        StorageAccessManager(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "SETTINGS",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Manage your VAULTIQ app preferences and storage.",
            style = MaterialTheme.typography.bodyMedium
        )

        StorageSettingsSection(
            storageAccessManager = storageAccessManager
        )
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
