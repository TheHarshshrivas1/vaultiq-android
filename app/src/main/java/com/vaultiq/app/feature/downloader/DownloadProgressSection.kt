
package com.vaultiq.app.feature.downloader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.WorkInfo
import com.vaultiq.app.download.queue.DownloadProgressObserver
import com.vaultiq.app.download.queue.DownloadProgressInfo
import com.vaultiq.app.ui.theme.VaultGold
import com.vaultiq.app.ui.theme.VaultMuted
import com.vaultiq.app.ui.theme.VaultNavy
import com.vaultiq.app.ui.theme.VaultPanel
import com.vaultiq.app.ui.theme.VaultSilver
import com.vaultiq.app.ui.theme.VaultWhite

@Composable
fun DownloadProgressSection() {
    val context = LocalContext.current

    val observer = androidx.compose.runtime.remember {
        DownloadProgressObserver(context)
    }

    val downloads by observer.observeAll()
        .collectAsState(initial = emptyList())

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "DOWNLOAD ACTIVITY",
            color = VaultGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )

        if (downloads.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = VaultPanel,
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = "No download activity yet.",
                    color = VaultMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            downloads
                .sortedByDescending { it.downloadId }
                .forEach { download ->
                    DownloadProgressCard(download)
                }
        }
    }
}

@Composable
private fun DownloadProgressCard(
    download: DownloadProgressInfo
) {
    val status = when (download.state) {
        WorkInfo.State.ENQUEUED -> "QUEUED"
        WorkInfo.State.RUNNING -> "DOWNLOADING"
        WorkInfo.State.SUCCEEDED -> "COMPLETED"
        WorkInfo.State.FAILED -> "FAILED"
        WorkInfo.State.CANCELLED -> "CANCELLED"
        WorkInfo.State.BLOCKED -> "WAITING"
    }

    val percentage = download.percentage
        .takeIf { it in 0..100 }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = VaultPanel,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "DOWNLOAD #${download.downloadId}",
                    color = VaultWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = status,
                    color = VaultGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (download.state == WorkInfo.State.RUNNING) {
                if (percentage != null) {
                    LinearProgressIndicator(
                        progress = percentage / 100f,
                        modifier = Modifier.fillMaxWidth(),
                        color = VaultGold,
                        trackColor = VaultNavy
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = VaultGold,
                        trackColor = VaultNavy
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = percentage?.let { "$it%" }
                        ?: "Calculating progress...",
                    color = VaultSilver,
                    fontSize = 11.sp
                )
            }

            if (download.totalBytes > 0L) {
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${formatProgressBytes(download.downloadedBytes)} / ${formatProgressBytes(download.totalBytes)}",
                    color = VaultMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

private fun formatProgressBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"

    val units = listOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = -1

    while (value >= 1024.0 && index < units.lastIndex) {
        value /= 1024.0
        index++
    }

    return String.format(
        java.util.Locale.US,
        "%.2f %s",
        value,
        units[index]
    )
}
