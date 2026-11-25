
package com.vaultiq.app.download.worker

import android.content.Context
import android.os.SystemClock
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.vaultiq.app.core.common.VaultResult
import com.vaultiq.app.data.database.DownloadEntity
import com.vaultiq.app.data.database.VaultDatabase
import com.vaultiq.app.data.repository.DownloadRepository
import com.vaultiq.app.download.DownloadStatus
import com.vaultiq.app.download.engine.DownloadDestinationManager
import com.vaultiq.app.download.engine.DownloadEngine
import com.vaultiq.app.download.queue.DownloadQueue
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class DownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val repository = DownloadRepository(
        VaultDatabase.getInstance(appContext).downloadDao()
    )

    private val engine = DownloadEngine()

    private val destinationManager =
        DownloadDestinationManager(appContext)

    override suspend fun doWork(): Result {
        val downloadId = inputData.getLong(
            DownloadQueue.INPUT_DOWNLOAD_ID,
            -1L
        )

        val url = inputData.getString(
            DownloadQueue.INPUT_URL
        ) ?: return Result.failure()

        if (downloadId <= 0L) {
            return Result.failure()
        }

        val record = when (
            val result = repository.getById(downloadId)
        ) {
            is VaultResult.Success -> result.data
            is VaultResult.Error -> return Result.failure()
            is VaultResult.Loading -> return Result.failure()
        }

        if (record.url != url) {
            return Result.failure()
        }

        if (!updateRecord(downloadId) {
                it.copy(
                    status = DownloadStatus.RUNNING.name,
                    errorMessage = null,
                    downloadedBytes = 0L,
                    totalBytes = 0L,
                    filePath = null
                )
            }
        ) {
            return Result.failure()
        }

        return try {
            setProgress(
                workDataOf(
                    "download_id" to downloadId,
                    "downloaded_bytes" to 0L,
                    "total_bytes" to 0L,
                    "progress_percent" to -1
                )
            )

            var lastProgressUpdate = 0L
            var lastProgressBytes = 0L
            var hasReportedProgress = false

            val result = engine.download(
                url = url,
                destinationFactory = { fileName, mimeType ->
                    destinationManager.createDestination(
                        fileName = fileName,
                        mimeType = mimeType
                    )
                },
                cleanupDestination = { uri ->
                    destinationManager.deleteDestination(uri)
                },
                onProgress = { progress ->
                    val now = SystemClock.elapsedRealtime()

                    val shouldReport =
                        !hasReportedProgress ||
                        now - lastProgressUpdate >= 500L ||
                        progress.downloadedBytes - lastProgressBytes >=
                            512L * 1024L

                    if (shouldReport) {
                        hasReportedProgress = true
                        lastProgressUpdate = now
                        lastProgressBytes = progress.downloadedBytes

                        setProgressAsync(
                            workDataOf(
                                "download_id" to downloadId,
                                "downloaded_bytes" to
                                    progress.downloadedBytes,
                                "total_bytes" to progress.totalBytes,
                                "progress_percent" to
                                    (progress.percentage ?: -1)
                            )
                        )
                    }
                }
            )

            when (result) {
                is VaultResult.Success -> {
                    val summary = result.data

                    val saved = updateRecord(downloadId) {
                        it.copy(
                            fileName = summary.fileName,
                            mimeType = summary.mimeType,
                            filePath = summary.destinationUri.toString(),
                            totalBytes = summary.totalBytes,
                            downloadedBytes = summary.bytesDownloaded,
                            status = DownloadStatus.COMPLETED.name,
                            errorMessage = null
                        )
                    }

                    if (saved) {
                        Result.success(
                            workDataOf(
                                "download_id" to downloadId,
                                "completed" to true,
                                "file_uri" to
                                    summary.destinationUri.toString()
                            )
                        )
                    } else {
                        destinationManager.deleteDestination(
                            summary.destinationUri
                        )

                        updateRecord(downloadId) {
                            it.copy(
                                status = DownloadStatus.FAILED.name,
                                filePath = null,
                                downloadedBytes = 0L,
                                totalBytes = 0L,
                                errorMessage =
                                    "Download completed but history could not be saved."
                            )
                        }

                        Result.failure()
                    }
                }

                is VaultResult.Error -> {
                    val retryable = isRetryableError(result)

                    if (retryable && runAttemptCount < MAX_RETRIES) {
                        updateRecord(downloadId) {
                            it.copy(
                                status = DownloadStatus.QUEUED.name,
                                downloadedBytes = 0L,
                                totalBytes = 0L,
                                filePath = null,
                                errorMessage = result.message
                            )
                        }

                        Result.retry()
                    } else {
                        updateRecord(downloadId) {
                            it.copy(
                                status = DownloadStatus.FAILED.name,
                                downloadedBytes = 0L,
                                totalBytes = 0L,
                                filePath = null,
                                errorMessage = result.message
                            )
                        }

                        Result.failure()
                    }
                }

                is VaultResult.Loading -> {
                    updateRecord(downloadId) {
                        it.copy(
                            status = DownloadStatus.FAILED.name,
                            downloadedBytes = 0L,
                            totalBytes = 0L,
                            filePath = null,
                            errorMessage =
                                "Download could not be completed."
                        )
                    }

                    Result.failure()
                }
            }
        } catch (exception: CancellationException) {
            withContext(NonCancellable) {
                updateRecord(downloadId) {
                    it.copy(
                        status = DownloadStatus.CANCELLED.name,
                        downloadedBytes = 0L,
                        totalBytes = 0L,
                        filePath = null,
                        errorMessage = "Download was cancelled."
                    )
                }
            }

            throw exception
        } catch (exception: Exception) {
            withContext(NonCancellable) {
                updateRecord(downloadId) {
                    it.copy(
                        status = DownloadStatus.FAILED.name,
                        downloadedBytes = 0L,
                        totalBytes = 0L,
                        filePath = null,
                        errorMessage = exception.message
                            ?: "Unexpected download error."
                    )
                }
            }

            Result.failure()
        }
    }

    private fun isRetryableError(
        error: VaultResult.Error
    ): Boolean {
        if (error.cause is IOException) {
            return true
        }

        val httpCode = Regex(
            """HTTP\s+(\d{3})""",
            RegexOption.IGNORE_CASE
        )
            .find(error.message)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()

        return httpCode == 408 ||
            httpCode == 425 ||
            httpCode == 429 ||
            (httpCode != null && httpCode in 500..599)
    }

    private suspend fun updateRecord(
        downloadId: Long,
        transform: (DownloadEntity) -> DownloadEntity
    ): Boolean {
        val current = when (
            val result = repository.getById(downloadId)
        ) {
            is VaultResult.Success -> result.data
            is VaultResult.Error -> return false
            is VaultResult.Loading -> return false
        }

        return repository.update(
            transform(current)
        ) is VaultResult.Success
    }

    companion object {
        private const val MAX_RETRIES = 3
    }
}
