package com.vaultiq.app.download.worker

import android.content.Context
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

        val recordResult = repository.getById(downloadId)

        val record = when (recordResult) {
            is VaultResult.Success -> recordResult.data
            is VaultResult.Error -> return Result.failure()
            is VaultResult.Loading -> return Result.failure()
        }

        if (record.url != url) {
            return Result.failure()
        }

        val startUpdated = updateRecord(downloadId) {
            it.copy(
                status = DownloadStatus.RUNNING.name,
                errorMessage = null,
                downloadedBytes = 0L,
                totalBytes = 0L,
                filePath = null
            )
        }

        if (!startUpdated) {
            return Result.failure()
        }

        return try {
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
                    setProgressAsync(
                        workDataOf(
                            "download_id" to downloadId,
                            "downloaded_bytes" to progress.downloadedBytes,
                            "total_bytes" to progress.totalBytes,
                            "progress_percent" to
                                (progress.percentage ?: -1)
                        )
                    )
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
                                "file_uri" to summary.destinationUri.toString()
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
                    val canRetry =
                        result.cause is IOException &&
                            runAttemptCount < 3

                    if (canRetry) {
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
            updateRecord(downloadId) {
                it.copy(
                    status = DownloadStatus.FAILED.name,
                    errorMessage = exception.message
                        ?: "Unexpected download error."
                )
            }

            Result.failure()
        }
    }

    private suspend fun updateRecord(
        downloadId: Long,
        transform: (DownloadEntity) -> DownloadEntity
    ): Boolean {
        val currentResult = repository.getById(downloadId)

        val current = when (currentResult) {
            is VaultResult.Success -> currentResult.data
            is VaultResult.Error -> return false
            is VaultResult.Loading -> return false
        }

        return repository.update(transform(current)) is VaultResult.Success
    }
}
