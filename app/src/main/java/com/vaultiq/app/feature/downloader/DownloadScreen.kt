
package com.vaultiq.app.feature.downloader

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultiq.app.ui.designsystem.VaultInfoCard
import com.vaultiq.app.ui.designsystem.VaultPill
import com.vaultiq.app.ui.designsystem.VaultSectionTitle
import com.vaultiq.app.ui.theme.VaultBorder
import com.vaultiq.app.ui.theme.VaultGold
import com.vaultiq.app.ui.theme.VaultMuted
import com.vaultiq.app.ui.theme.VaultNavy
import com.vaultiq.app.ui.theme.VaultPanel
import com.vaultiq.app.ui.theme.VaultSilver
import com.vaultiq.app.ui.theme.VaultWhite
import java.net.URI

private val downloadTabs = listOf(
    "Search",
    "YouTube",
    "Music",
    "More"
)

@Composable
fun DownloadScreen(
    sharedUrl: String? = null,
    shareError: String? = null,
    onClearSharedContent: () -> Unit = {}
) {
    var selectedTab by rememberSaveable {
        mutableStateOf("Search")
    }

    var mediaUrl by rememberSaveable {
        mutableStateOf("")
    }

    LaunchedEffect(sharedUrl, shareError) {
        if (sharedUrl != null) {
            mediaUrl = sharedUrl
            selectedTab = "Search"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultNavy)
    ) {
        DownloadHeader()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
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

        Spacer(modifier = Modifier.height(14.dp))

        when (selectedTab) {
            "Search" -> SearchContent(
                mediaUrl = mediaUrl,
                onUrlChange = { mediaUrl = it },
                sharedUrl = sharedUrl,
                shareError = shareError,
                onClearSharedContent = onClearSharedContent
            )

            "YouTube" -> SourceContent(
                title = "YouTube",
                symbol = "▶",
                description = "Share a link to VAULTIQ or use a supported media URL. Download availability depends on source permissions."
            )

            "Music" -> SourceContent(
                title = "Music",
                symbol = "♫",
                description = "Keep your permitted audio downloads organized in your personal media vault."
            )

            "More" -> MoreContent()
        }
    }
}

@Composable
private fun DownloadHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
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
            modifier = Modifier.size(42.dp),
            shape = CircleShape,
            color = VaultPanel
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "V",
                    color = VaultGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
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
            .padding(horizontal = 5.dp, vertical = 7.dp),
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
    onUrlChange: (String) -> Unit,
    sharedUrl: String?,
    shareError: String?,
    onClearSharedContent: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(42.dp))

        Text(
            text = "YOUR MEDIA.",
            color = VaultWhite,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "ONE PERSONAL VAULT.",
            color = VaultGold,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(26.dp))

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
                    text = "↗",
                    color = VaultGold,
                    fontSize = 22.sp
                )
            },
            shape = RoundedCornerShape(28.dp),
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

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Paste a link or share content from another app to VAULTIQ.",
            color = VaultMuted,
            fontSize = 11.sp
        )

        if (sharedUrl != null) {
            Spacer(modifier = Modifier.height(22.dp))

            SmartPreviewCard(
                url = sharedUrl,
                onClose = onClearSharedContent
            )
        }

        if (shareError != null) {
            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = VaultPanel,
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "SHARED LINK NOT ACCEPTED",
                        color = VaultGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = shareError,
                        color = VaultSilver,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "DISMISS",
                        color = VaultGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            onClearSharedContent()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        if (sharedUrl == null) {
            VaultPill(text = "SMART PREVIEW")

            Spacer(modifier = Modifier.height(14.dp))

            VaultInfoCard(
                title = "Preview before download",
                description = "Review supported media details and available options before saving.",
                symbol = "◉"
            )

            Spacer(modifier = Modifier.height(12.dp))

            VaultInfoCard(
                title = "Personal media library",
                description = "Keep your saved videos, music, images and files organized.",
                symbol = "▣"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        VaultSectionTitle(
            title = "QUICK ACCESS",
            subtitle = "Choose a section from the tabs above."
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun SmartPreviewCard(
    url: String,
    onClose: () -> Unit
) {
    val source = runCatching {
        URI(url).host?.removePrefix("www.") ?: "Unknown source"
    }.getOrDefault("Unknown source")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = VaultPanel,
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SMART PREVIEW",
                    color = VaultGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "LINK RECEIVED",
                    color = VaultSilver,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Source",
                color = VaultMuted,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = source,
                color = VaultWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Shared URL",
                color = VaultMuted,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = url,
                color = VaultSilver,
                fontSize = 11.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                color = VaultNavy,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Download availability and media options have not been checked yet.",
                    color = VaultSilver,
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "CLOSE PREVIEW",
                color = VaultGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable(onClick = onClose)
                    .padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun SourceContent(
    title: String,
    symbol: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = symbol,
            color = VaultGold,
            fontSize = 55.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            color = VaultWhite,
            fontSize = 25.sp,
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
        VaultSectionTitle(
            title = "EXPLORE SOURCES",
            subtitle = "Sharing and download support varies by platform."
        )

        platforms.forEach { platform ->
            VaultInfoCard(
                title = platform.first,
                description = "View sharing guidance and supported options.",
                symbol = platform.second
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}
