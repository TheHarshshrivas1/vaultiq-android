
package com.vaultiq.app.download.queue

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class DownloadProgressInfo(
    val downloadId: Long,
    val state: WorkInfo.State,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val percentage: Int
)

class DownloadProgressObserver(context: Context) {

    private val workManager = WorkManager.getInstance(
        context.applicationContext
    )

    fun observe(downloadId: Long): Flow<DownloadProgressInfo?> {
        return workManager
            .getWorkInfosForUniqueWorkFlow(
                DownloadQueue.uniqueWorkName(downloadId)
            )
            .map { workInfos ->

                val workInfo = workInfos.firstOrNull()
                    ?: return@map null

                val progress = workInfo.progress

                DownloadProgressInfo(
                    downloadId = downloadId,
                    state = workInfo.state,
                    downloadedBytes = progress.getLong(
                        "downloaded_bytes",
                        0L
                    ),
                    totalBytes = progress.getLong(
                        "total_bytes",
                        0L
                    ),
                    percentage = progress.getInt(
                        "progress_percent",
                        -1
                    )
                )
            }
    }
}
