
package com.vaultiq.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vaultiq.app.core.common.VaultResult
import com.vaultiq.app.core.navigation.VaultNavGraph
import com.vaultiq.app.core.navigation.VaultRoutes
import com.vaultiq.app.core.utils.SharedUrlParser
import com.vaultiq.app.ui.designsystem.VaultBottomBar
import com.vaultiq.app.ui.theme.VaultIQTheme

class MainActivity : ComponentActivity() {

    private val pendingSharedUrl = mutableStateOf<String?>(null)
    private val sharedLinkError = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleSharedIntent(intent)

        setContent {
            VaultIQTheme {
                val navController = rememberNavController()
                val sharedUrl by pendingSharedUrl
                val shareError by sharedLinkError

                val backStackEntry by
                    navController.currentBackStackEntryAsState()

                val currentRoute =
                    backStackEntry?.destination?.route
                        ?: VaultRoutes.DOWNLOAD

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        VaultBottomBar(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(
                                        navController.graph.startDestinationId
                                    ) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    VaultNavGraph(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding),
                        sharedUrl = sharedUrl,
                        shareError = shareError,
                        onClearSharedContent = {
                            pendingSharedUrl.value = null
                            sharedLinkError.value = null
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSharedIntent(intent)
    }

    private fun handleSharedIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return

        if (intent.type != "text/plain") {
            sharedLinkError.value = "Unsupported shared content type."
            pendingSharedUrl.value = null
            return
        }

        when (
            val result = SharedUrlParser.extractUrl(
                intent.getStringExtra(Intent.EXTRA_TEXT)
            )
        ) {
            is VaultResult.Success -> {
                pendingSharedUrl.value = result.data.toString()
                sharedLinkError.value = null
            }

            is VaultResult.Error -> {
                pendingSharedUrl.value = null
                sharedLinkError.value = result.message
            }

            is VaultResult.Loading -> {
                pendingSharedUrl.value = null
                sharedLinkError.value =
                    "Please try sharing the link again."
            }
        }
    }
}
