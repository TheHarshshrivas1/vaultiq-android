
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.WorkInfo
import com.vaultiq.app.data.database.DownloadEntity
import com.vaultiq.app.data.database.VaultDatabase
import com.vaultiq.app.data.repository.DownloadRepository
import com.vaultiq.app.download.queue.DownloadProgressInfo
import com.vaultiq.app.download.queue.DownloadProgressObserver
import com.vaultiq.app.ui.theme.VaultGold
import com.vaultiq.app.ui.theme.VaultMuted
import com.vaultiq.app.ui.theme.VaultNavy
import com.vaultiq.app.ui.theme.VaultPanel
import com.vaultiq.app.ui.theme.VaultSilver
import com.vaultiq.app.ui.theme.VaultWhite
import kotlinx.coroutines.flow.combine

private data class DownloadActivityItem(
    val record: DownloadEntity,
    val progress: DownloadProgressInfo?
)

@Composable
fun DownloadProgressSection() {
    val context = LocalContext.current

    val repository = remember {
        DownloadRepository(
            VaultDatabase.getInstance(context).downloadDao()
        )
    }

    val observer = remember {
        DownloadProgressObserver(context)
    }

    val activityFlow = remember {
        combine(
            repository.observeAll(),
            observer.observeAll()
        ) { records, progressItems ->

            val progressById = progressItems.associateBy {
                it.downloadId
            }

            records.map { record ->
                DownloadActivityItem(
                    record = record,
                    progress = progressById[record.id]
                )
            }
        }
    }

    val downloads by activityFlow.collectAsState(
        initial = emptyList()
    )

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
            downloads.forEach { item ->
                DownloadProgressCard(item)
            }
        }
    }
}

@Composable
private fun DownloadProgressCard(
    item: DownloadActivityItem
) {
    val record = item.record
    val progress = item.progress

    val status = when (progress?.state) {
        WorkInfo.State.ENQUEUED -> "QUEUED"
        WorkInfo.State.RUNNING -> "DOWNLOADING"
        WorkInfo.State.SUCCEEDED -> "COMPLETED"
        WorkInfo.State.FAILED -> "FAILED"
        WorkInfo.State.CANCELLED -> "CANCELLED"
        WorkInfo.State.BLOCKED -> "WAITING"
        null -> record.status
    }

    val downloadedBytes =
        progress?.downloadedBytes?.takeIf { it > 0L }
            ?: record.downloadedBytes

    val totalBytes =
        progress?.totalBytes?.takeIf { it > 0L }
            ?: record.totalBytes

    val percentage = when {
        progress?.state == WorkInfo.State.SUCCEEDED -> 100
        record.status == "COMPLETED" -> 100
        progress?.percentage?.takeIf { it in 0..100 } != null ->
    progress.percentage
        else -> null
    }

    val isRunning =
        progress?.state == WorkInfo.State.RUNNING ||
            (progress == null && record.status == "RUNNING")

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
                    text = "DOWNLOAD #${record.id}",
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

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = record.fileName,
                color = VaultSilver,
                fontSize = 12.sp
            )

            if (isRunning) {
                Spacer(modifier = Modifier.height(12.dp))

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

            if (totalBytes > 0L) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${formatProgressBytes(downloadedBytes)} / ${formatProgressBytes(totalBytes)}",
                    color = VaultMuted,
                    fontSize = 10.sp
                )
            }

            if (!record.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = record.errorMessage,
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
