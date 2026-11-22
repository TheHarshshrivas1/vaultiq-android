
package com.vaultiq.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vaultiq.app.feature.downloader.DownloadScreen
import com.vaultiq.app.feature.library.PlayScreen
import com.vaultiq.app.feature.settings.SettingsScreen

object VaultRoutes {
    const val DOWNLOAD = "download"
    const val PLAY = "play"
    const val SETTINGS = "settings"

    const val HOME = DOWNLOAD
    const val LIBRARY = PLAY
    const val HISTORY = "history"
}

@Composable
fun VaultNavGraph(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
    sharedUrl: String? = null,
    shareError: String? = null,
    onClearSharedContent: () -> Unit = {}
) {

    LaunchedEffect(sharedUrl, shareError) {
        if (sharedUrl != null || shareError != null) {
            navController.navigate(VaultRoutes.DOWNLOAD) {
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = VaultRoutes.DOWNLOAD,
        modifier = modifier
    ) {
        composable(VaultRoutes.DOWNLOAD) {
            DownloadScreen(
                sharedUrl = sharedUrl,
                shareError = shareError,
                onClearSharedContent = onClearSharedContent
            )
        }

        composable(VaultRoutes.PLAY) {
            PlayScreen()
        }

        composable(VaultRoutes.SETTINGS) {
            SettingsScreen()
        }
    }
}
