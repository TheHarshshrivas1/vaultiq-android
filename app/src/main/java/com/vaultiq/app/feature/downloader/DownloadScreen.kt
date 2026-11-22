
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultiq.app.core.common.VaultResult
import com.vaultiq.app.core.utils.MediaSourceDetector
import com.vaultiq.app.core.utils.MediaType
import com.vaultiq.app.download.engine.DirectMediaInspection
import com.vaultiq.app.download.engine.DirectMediaInspector
import com.vaultiq.app.download.queue.DownloadRequestManager
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
import kotlinx.coroutines.launch

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

    var inspection by remember {
        mutableStateOf<DirectMediaInspection?>(null)
    }

    var inspectionError by remember {
        mutableStateOf<String?>(null)
    }

    var isInspecting by remember {
        mutableStateOf(false)
    }

    var isSubmitting by remember {
        mutableStateOf(false)
    }

    var downloadMessage by remember {
        mutableStateOf<String?>(null)
    }

    val inspector = remember {
        DirectMediaInspector()
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val inspectUrl: suspend (String) -> Unit = { candidate ->

    inspection = null
    inspectionError = null
    downloadMessage = null

    isInspecting = true

    try {
        when (val result = inspector.inspect(candidate)) {

            is VaultResult.Success -> {
                inspection = result.data
            }

            is VaultResult.Error -> {
                inspectionError = result.message
            }

            is VaultResult.Loading -> {
                inspectionError = "Inspection is still in progress."
            }
        }
    } catch (exception: kotlinx.coroutines.CancellationException) {
        throw exception
    } catch (exception: Exception) {
        inspectionError = exception.message
            ?: "Unable to inspect this link."
    } finally {
        isInspecting = false
    }
    }

    LaunchedEffect(sharedUrl) {
        if (sharedUrl != null) {
            mediaUrl = sharedUrl
            selectedTab = "Search"
            inspectUrl(sharedUrl)
        } else {
            inspection = null
            inspectionError = null
            downloadMessage = null
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
                onUrlChange = {
                    mediaUrl = it
                    inspection = null
                    inspectionError = null
                    downloadMessage = null
                },
                sharedUrl = sharedUrl,
                shareError = shareError,
                inspection = inspection,
                inspectionError = inspectionError,
                isInspecting = isInspecting,
                isSubmitting = isSubmitting,
                downloadMessage = downloadMessage,
                onInspect = {
                    scope.launch {
                        inspectUrl(mediaUrl)
                    }
                },
                onDownload = {
                    val readyUrl = inspection?.url

                    if (readyUrl != null && !isSubmitting) {
                        scope.launch {
                            isSubmitting = true
                            downloadMessage = null

                            when (
                                val result = DownloadRequestManager(context)
                                    .submit(readyUrl)
                            ) {
                                is VaultResult.Success -> {
                                    downloadMessage =
                                        "Added to download queue. ID: ${result.data}"
                                }

                                is VaultResult.Error -> {
                                    downloadMessage = result.message
                                }

                                is VaultResult.Loading -> {
                                    downloadMessage =
                                        "Please try adding the download again."
                                }
                            }

                            isSubmitting = false
                        }
                    }
                },
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
    inspection: DirectMediaInspection?,
    inspectionError: String?,
    isInspecting: Boolean,
    isSubmitting: Boolean,
    downloadMessage: String?,
    onInspect: () -> Unit,
    onDownload: () -> Unit,
    onClearSharedContent: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(36.dp))

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

        Button(
            onClick = onInspect,
            enabled = mediaUrl.isNotBlank() && !isInspecting,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VaultGold,
                contentColor = VaultNavy,
                disabledContainerColor = VaultBorder,
                disabledContentColor = VaultMuted
            )
        ) {
            if (isInspecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = VaultNavy,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("CHECKING LINK")
            } else {
                Text(
                    text = "CHECK LINK",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Paste a link or share content from another app to VAULTIQ.",
            color = VaultMuted,
            fontSize = 11.sp
        )

        if (sharedUrl != null) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "LINK RECEIVED FROM ANOTHER APP",
                color = VaultGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "You can close this preview without affecting the original app.",
                color = VaultMuted,
                fontSize = 10.sp,
                modifier = Modifier
                    .clickable(onClick = onClearSharedContent)
                    .padding(8.dp)
            )
        }

        if (shareError != null) {
            Spacer(modifier = Modifier.height(16.dp))

            MessageCard(
                title = "SHARED LINK NOT ACCEPTED",
                message = shareError
            )
        }

        if (isInspecting) {
            Spacer(modifier = Modifier.height(22.dp))

            CircularProgressIndicator(
                color = VaultGold,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "INSPECTING MEDIA LINK...",
                color = VaultSilver,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (inspection != null) {
            Spacer(modifier = Modifier.height(22.dp))

            SmartPreviewCard(
                inspection = inspection,
                onDownload = onDownload,
                isSubmitting = isSubmitting,
                downloadMessage = downloadMessage
            )
        }

        if (inspectionError != null) {
            Spacer(modifier = Modifier.height(18.dp))

            MessageCard(
                title = "PREVIEW UNAVAILABLE",
                message = inspectionError
            )
        }

        Spacer(modifier = Modifier.height(26.dp))

        if (sharedUrl == null && inspection == null) {
            VaultPill(text = "SMART PREVIEW")

            Spacer(modifier = Modifier.height(14.dp))

            VaultInfoCard(
                title = "Preview before download",
                description = "Check supported media details and available options before saving.",
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
    inspection: DirectMediaInspection,
    onDownload: () -> Unit,
    isSubmitting: Boolean,
    downloadMessage: String?
) {
    val source = when (
        val result = MediaSourceDetector.detect(inspection.url)
    ) {
        is VaultResult.Success -> result.data.source.displayName
        else -> "Direct Web Link"
    }

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
                    text = "INSPECTED",
                    color = VaultGold,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            PreviewDetail("SOURCE", source)
            PreviewDetail(
                "MEDIA TYPE",
                inspection.media.type.displayName
            )
            PreviewDetail(
                "FORMAT",
                inspection.media.extension
                    ?.uppercase()
                    ?: inspection.contentType
                    ?: "Unknown"
            )
            PreviewDetail(
                "MIME TYPE",
                inspection.contentType ?: "Not provided"
            )
            PreviewDetail(
                "FILE SIZE",
                inspection.contentLength?.let { formatBytes(it) }
                    ?: "Not provided by server"
            )
            PreviewDetail(
                "HTTP STATUS",
                inspection.httpCode.toString()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "AVAILABLE FORMAT",
                color = VaultGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = VaultNavy,
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Text(
                        text = inspection.media.type.displayName,
                        color = VaultWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = inspection.media.extension
                            ?.uppercase()
                            ?: "Server-provided format",
                        color = VaultMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Only the format confirmed by the server is shown. Multiple quality options require a source that provides multiple formats.",
                color = VaultMuted,
                fontSize = 10.sp,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onDownload,
                enabled = !isSubmitting && inspection.isMediaFile,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VaultGold,
                    contentColor = VaultNavy
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = VaultNavy,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("ADDING...")
                } else {
                    Text(
                        text = "ADD TO DOWNLOADS",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (downloadMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = downloadMessage,
                    color = VaultSilver,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun PreviewDetail(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Text(
            text = label,
            color = VaultMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            color = VaultWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MessageCard(
    title: String,
    message: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = VaultPanel,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                color = VaultGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                color = VaultSilver,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"

    val units = listOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = -1

    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }

    return String.format(
        java.util.Locale.US,
        "%.2f %s",
        value,
        units[unitIndex]
    )
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
