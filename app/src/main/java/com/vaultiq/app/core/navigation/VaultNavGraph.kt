
package com.vaultiq.app.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vaultiq.app.library.StorageAccessManager
import com.vaultiq.app.library.StorageSettingsSection
import com.vaultiq.app.ui.theme.VaultGold
import com.vaultiq.app.ui.theme.VaultMuted
import com.vaultiq.app.ui.theme.VaultPanel
import com.vaultiq.app.ui.theme.VaultPanelLight
import com.vaultiq.app.ui.theme.VaultSilver
import com.vaultiq.app.ui.theme.VaultSuccess
import com.vaultiq.app.ui.theme.VaultWhite

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
            HomeScreen()
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
private fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = "VAULTIQ",
                color = VaultGold,
                fontSize = 27.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp
            )

            Text(
                text = "CAPTURE. COLLECT. ELEVATE.",
                color = VaultSilver,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Medium
            )
        }

        HeroCard()

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeading("YOUR VAULT")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    symbol = "↓",
                    title = "Downloads",
                    value = "0",
                    subtitle = "Total saved",
                    accent = VaultGold
                )

                StatCard(
                    modifier = Modifier.weight(1f),
                    symbol = "▣",
                    title = "Media files",
                    value = "0",
                    subtitle = "In your library",
                    accent = VaultSuccess
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeading("EXPLORE MEDIA")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MediaCard(
                    modifier = Modifier.weight(1f),
                    symbol = "▶",
                    title = "Videos",
                    description = "Your video collection"
                )

                MediaCard(
                    modifier = Modifier.weight(1f),
                    symbol = "♫",
                    title = "Audio",
                    description = "Your audio collection"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MediaCard(
                    modifier = Modifier.weight(1f),
                    symbol = "▧",
                    title = "Images",
                    description = "Your image collection"
                )

                MediaCard(
                    modifier = Modifier.weight(1f),
                    symbol = "▤",
                    title = "Files",
                    description = "Other saved files"
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeading("RECENT ACTIVITY")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = VaultPanel
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text(
                        text = "◷",
                        color = VaultGold,
                        fontSize = 32.sp
                    )

                    Text(
                        text = "Nothing here yet",
                        color = VaultWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )

                    Text(
                        text = "Your recent downloads will appear here.",
                        color = VaultMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Text(
            text = "YOUR PERSONAL MEDIA VAULT",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 10.dp),
            color = VaultMuted,
            fontSize = 9.sp,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun HeroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = VaultPanelLight
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = VaultGold.copy(alpha = 0.13f)
            ) {
                Text(
                    text = "YOUR PERSONAL MEDIA VAULT",
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 7.dp
                    ),
                    color = VaultGold,
                    fontSize = 9.sp,
                    letterSpacing = 1.3.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Everything you\nwant to keep.",
                color = VaultWhite,
                fontSize = 27.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Collect and organize media you are authorized to save, all in one private place.",
                color = VaultSilver,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(
                            VaultSuccess,
                            RoundedCornerShape(50.dp)
                        )
                )

                Text(
                    text = "PRIVATE • ORGANIZED • YOURS",
                    color = VaultSilver,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String) {
    Text(
        text = title,
        color = VaultWhite,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp
    )
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    symbol: String,
    title: String,
    value: String,
    subtitle: String,
    accent: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(
            containerColor = VaultPanel
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = symbol,
                color = accent,
                fontSize = 23.sp
            )

            Text(
                text = value,
                color = VaultWhite,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = title,
                color = VaultWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = subtitle,
                color = VaultMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun MediaCard(
    modifier: Modifier = Modifier,
    symbol: String,
    title: String,
    description: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(
            containerColor = VaultPanel
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                text = symbol,
                color = VaultGold,
                fontSize = 25.sp
            )

            Text(
                text = title,
                color = VaultWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = description,
                color = VaultMuted,
                fontSize = 10.sp,
                lineHeight = 15.sp
            )
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
