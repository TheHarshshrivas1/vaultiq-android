
package com.vaultiq.app.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vaultiq.app.library.StorageAccessManager
import com.vaultiq.app.library.StorageSettingsSection
import com.vaultiq.app.ui.theme.VaultBorder
import com.vaultiq.app.ui.theme.VaultGold
import com.vaultiq.app.ui.theme.VaultMuted
import com.vaultiq.app.ui.theme.VaultNavy
import com.vaultiq.app.ui.theme.VaultPanel
import com.vaultiq.app.ui.theme.VaultPanelLight
import com.vaultiq.app.ui.theme.VaultSilver
import com.vaultiq.app.ui.theme.VaultWhite

object VaultRoutes {
    const val DOWNLOAD = "download"
    const val PLAY = "play"
    const val SETTINGS = "settings"

    // Compatibility aliases for older internal references.
    const val HOME = DOWNLOAD
    const val LIBRARY = PLAY
    const val HISTORY = "history"
}

private val downloadTabs = listOf(
    "Search",
    "YouTube",
    "Music",
    "More"
)

@Composable
fun VaultNavGraph(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = VaultRoutes.DOWNLOAD,
        modifier = modifier
    ) {
        composable(VaultRoutes.DOWNLOAD) {
            DownloadScreen()
        }

        composable(VaultRoutes.PLAY) {
            PlayScreen()
        }

        composable(VaultRoutes.SETTINGS) {
            SettingsScreen()
        }
    }
}

@Composable
private fun DownloadScreen() {
    var selectedTab by rememberSaveable {
        mutableStateOf("Search")
    }

    var mediaUrl by rememberSaveable {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultNavy)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "VAULTIQ",
                        color = VaultGold,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.5.sp
                    )
                    Text(
                        text = "CAPTURE. COLLECT. ELEVATE.",
                        color = VaultMuted,
                        fontSize = 9.sp,
                        letterSpacing = 1.5.sp
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = VaultPanel,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "V",
                            color = VaultGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                downloadTabs.forEach { tab ->
                    DownloadTab(
                        title = tab,
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        when (selectedTab) {
            "Search" -> SearchContent(
                mediaUrl = mediaUrl,
                onUrlChange = { mediaUrl = it }
            )

            "YouTube" -> SourceInfoContent(
                title = "YouTube",
                description = "Open a supported link or share content to VAULTIQ. Availability depends on the source and its permissions.",
                symbol = "▶"
            )

            "Music" -> SourceInfoContent(
                title = "Music",
                description = "Organize and save audio files from sources that permit downloading.",
                symbol = "♫"
            )

            "More" -> MoreContent()
        }
    }
}

@Composable
private fun DownloadTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = if (selected) VaultWhite else VaultMuted,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .width(if (selected) 30.dp else 12.dp)
                .height(3.dp)
                .background(
                    color = if (selected) VaultGold else Color.Transparent,
                    shape = RoundedCornerShape(10.dp)
                )
        )
    }
}

@Composable
private fun SearchContent(
    mediaUrl: String,
    onUrlChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Text(
            text = "VAULTIQ",
            color = VaultGold,
            fontSize = 37.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )

        Text(
            text = "YOUR PERSONAL MEDIA VAULT",
            color = VaultSilver,
            fontSize = 10.sp,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(34.dp))

        OutlinedTextField(
            value = mediaUrl,
            onValueChange = onUrlChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = {
                Text(
                    text = "Paste a supported media URL",
                    color = VaultMuted,
                    fontSize = 12.sp
                )
            },
            leadingIcon = {
                Text(
                    text = "↓",
                    color = VaultSilver,
                    fontSize = 22.sp
                )
            },
            shape = RoundedCornerShape(30.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = VaultWhite,
                unfocusedTextColor = VaultWhite,
                focusedBorderColor = VaultGold,
                unfocusedBorderColor = VaultBorder,
                cursorColor = VaultGold,
                focusedContainerColor = VaultPanel,
                unfocusedContainerColor = VaultPanel
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Paste a link or use Android Share to send content to VAULTIQ.",
            color = VaultMuted,
            fontSize = 11.sp,
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        InfoCard(
            title = "Smart Preview",
            description = "Review supported media information and available download options before saving.",
            symbol = "◉"
        )

        Spacer(modifier = Modifier.height(14.dp))

        InfoCard(
            title = "Your downloads, organized",
            description = "Manage supported downloads and keep your personal media collection together.",
            symbol = "▣"
        )
    }
}

@Composable
private fun SourceInfoContent(
    title: String,
    description: String,
    symbol: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(22.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = symbol,
            color = VaultGold,
            fontSize = 54.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            color = VaultWhite,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = description,
            color = VaultMuted,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun MoreContent() {
    val platforms = listOf(
        "YouTube" to "▶",
        "WhatsApp Status" to "◉",
        "Facebook" to "f",
        "TikTok" to "♪",
        "Instagram" to "◎",
        "Pinterest" to "P"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "SUPPORTED SOURCES",
            color = VaultSilver,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Bold
        )

        platforms.forEach { platform ->
            PlatformCard(
                title = platform.first,
                symbol = platform.second
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun PlatformCard(
    title: String,
    symbol: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(
            containerColor = VaultPanel
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(45.dp),
                shape = RoundedCornerShape(13.dp),
                color = VaultPanelLight
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = symbol,
                        color = VaultGold,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = VaultWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "View supported sharing options",
                    color = VaultMuted,
                    fontSize = 10.sp
                )
            }

            Text(
                text = "›",
                color = VaultSilver,
                fontSize = 25.sp
            )
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    description: String,
    symbol: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = VaultPanel
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = symbol,
                color = VaultGold,
                fontSize = 25.sp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    color = VaultWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = VaultMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun PlayScreen() {
    var selectedCategory by rememberSaveable {
        mutableStateOf("Videos")
    }

    val categories = listOf(
        "Videos",
        "Music",
        "Images",
        "Files"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultNavy)
            .padding(horizontal = 18.dp, vertical = 20.dp)
    ) {
        Text(
            text = "PLAY",
            color = VaultWhite,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "YOUR DOWNLOADED MEDIA",
            color = VaultMuted,
            fontSize = 10.sp,
            letterSpacing = 1.4.sp
        )

        Spacer(modifier = Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                CategoryChip(
                    title = category,
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "▣",
                    color = VaultGold,
                    fontSize = 42.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "No ${selectedCategory.lowercase()} yet",
                    color = VaultWhite,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Your saved ${selectedCategory.lowercase()} will appear here.",
                    color = VaultMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) VaultGold else VaultPanel
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 9.dp
            ),
            color = if (selected) VaultNavy else VaultSilver,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
            .background(VaultNavy)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "SETTINGS",
            color = VaultWhite,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "GENERAL",
            color = VaultMuted,
            fontSize = 10.sp,
            letterSpacing = 1.5.sp
        )

        InfoCard(
            title = "VAULTIQ",
            description = "CAPTURE. COLLECT. ELEVATE. — Your personal media vault.",
            symbol = "V"
        )

        Text(
            text = "STORAGE",
            color = VaultMuted,
            fontSize = 10.sp,
            letterSpacing = 1.5.sp
        )

        StorageSettingsSection(
            storageAccessManager = storageAccessManager
        )
    }
}
