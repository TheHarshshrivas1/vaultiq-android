
package com.vaultiq.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vaultiq.app.core.navigation.VaultNavGraph
import com.vaultiq.app.core.navigation.VaultRoutes
import com.vaultiq.app.ui.designsystem.VaultBottomBar
import com.vaultiq.app.ui.theme.VaultIQTheme

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
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
