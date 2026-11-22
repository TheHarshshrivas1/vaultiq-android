
package com.vaultiq.app.download.worker

import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.vaultiq.app.core.common.VaultResult
import com.vaultiq.app.data.database.DownloadEntity
import com.vaultiq.app.data.database.VaultDatabase
import com.vaultiq.app.data.repository.DownloadRepository
import com.vaultiq.app.download.DownloadStatus
import com.vaultiq.app.download.engine.DownloadEngine
import java.io.IOException

class DownloadWorker(
    appContext: android.content.Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val repository = DownloadRepository(
        VaultDatabase.getInstance(appContext).downloadDao()
    )

    private val engine = DownloadEngine()

    override suspend fun doWork(): Result {

        val downloadId = inputData.getLong(
            "download_id",
            -1L
        )

        val url = inputData.getString("download_url")
            ?: return Result.failure()

        val destinationValue =
            inputData.getString("destination_uri")
                ?: return Result.failure()

        if (downloadId <= 0L) {
            return Result.failure()
        }

        val destinationUri = try {
            Uri.parse(destinationValue)
        } catch (exception: Exception) {
            return Result.failure()
        }

        if (destinationUri.scheme != "content") {
            return Result.failure()
        }

        val recordResult = repository.getById(downloadId)

        val record = when (recordResult) {
            is VaultResult.Success -> recordResult.data
            else -> return Result.failure()
        }

        repository.update(
            record.copy(
                status = DownloadStatus.RUNNING.name,
                errorMessage = null,
                downloadedBytes = 0,
                totalBytes = 0
            )
        )

        return try {

            val resolver = applicationContext.contentResolver

            val outputStream = resolver.openOutputStream(
                destinationUri,
                "wt"
            ) ?: throw IOException(
                "Unable to open destination file."
            )

            val result = outputStream.use { stream ->

                engine.download(
                    url = url,
                    outputStream = stream
                ) { progress ->

                    setProgressAsync(
                        workDataOf(
                            "download_id" to downloadId,
                            "downloaded_bytes" to
                                progress.downloadedBytes,
                            "total_bytes" to
                                progress.totalBytes,
                            "progress_percent" to
                                (progress.percentage ?: -1)
                        )
                    )
                }
            }

            when (result) {

                is VaultResult.Success -> {

                    val summary = result.data

                    val updated = repository.update(
                        record.copy(
                            fileName = summary.fileName,
                            mimeType = summary.mimeType,
                            filePath = destinationUri.toString(),
                            totalBytes = summary.totalBytes,
                            downloadedBytes =
                                summary.bytesDownloaded,
                            status = DownloadStatus.COMPLETED.name,
                            errorMessage = null
                        )
                    )

                    if (updated is VaultResult.Success) {
                        Result.success(
                            workDataOf(
                                "download_id" to downloadId,
                                "completed" to true
                            )
                        )
                    } else {
                        Result.failure()
                    }
                }

                is VaultResult.Error -> {

                    val exception = result.cause

                    if (
                        exception is IOException &&
                        runAttemptCount < 3
                    ) {
                        repository.update(
                            record.copy(
                                status = DownloadStatus.QUEUED.name,
                                errorMessage = result.message
                            )
                        )

                        Result.retry()

                    } else {

                        repository.update(
                            record.copy(
                                status = DownloadStatus.FAILED.name,
                                errorMessage = result.message
                            )
                        )

                        Result.failure()
                    }
                }

                VaultResult.Loading -> {
                    Result.retry()
                }
            }

        } catch (exception: IOException) {

            repository.update(
                record.copy(
                    status = DownloadStatus.FAILED.name,
                    errorMessage = exception.message
                        ?: "Storage or network error."
                )
            )

            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }

        } catch (exception: Exception) {

            repository.update(
                record.copy(
                    status = DownloadStatus.FAILED.name,
                    errorMessage = exception.message
                        ?: "Unexpected download error."
                )
            )

            Result.failure()
        }
    }
}
