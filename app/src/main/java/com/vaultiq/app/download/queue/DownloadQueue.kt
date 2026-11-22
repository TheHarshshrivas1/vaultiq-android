
package com.vaultiq.app.download.queue

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import com.vaultiq.app.core.common.VaultResult
import com.vaultiq.app.core.utils.UrlValidator
import com.vaultiq.app.download.worker.DownloadWorker
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.ExecutionException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object DownloadQueue {

    const val INPUT_DOWNLOAD_ID = "download_id"
    const val INPUT_URL = "download_url"

    private const val WORK_PREFIX = "vaultiq_download_"
    private const val WORK_TAG = "vaultiq_download"

    private val directExecutor = Executor { command ->
        command.run()
    }

    suspend fun enqueue(
        context: Context,
        downloadId: Long,
        url: String
    ): VaultResult<Unit> {

        if (downloadId <= 0L) {
            return VaultResult.Error("Invalid download ID.")
        }

        val validation = UrlValidator.validate(url)

        if (validation !is VaultResult.Success) {
            return VaultResult.Error("Invalid download URL.")
        }

        return try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = androidx.work.Data.Builder()
                .putLong(INPUT_DOWNLOAD_ID, downloadId)
                .putString(INPUT_URL, validation.data.toString())
                .build()

            val request =
                OneTimeWorkRequestBuilder<DownloadWorker>()
                    .setConstraints(constraints)
                    .setInputData(inputData)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        30,
                        TimeUnit.SECONDS
                    )
                    .addTag(WORK_TAG)
                    .addTag("$WORK_TAG:$downloadId")
                    .build()

            val operation = WorkManager
                .getInstance(context.applicationContext)
                .enqueueUniqueWork(
                    uniqueWorkName(downloadId),
                    ExistingWorkPolicy.KEEP,
                    request
                )

            awaitOperation(operation)

            VaultResult.Success(Unit)

        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            VaultResult.Error(
                "Unable to add download to queue.",
                exception
            )
        }
    }

    private suspend fun awaitOperation(
        operation: Operation
    ) {
        suspendCancellableCoroutine<Unit> { continuation ->

            operation.result.addListener(
                {
                    try {
                        operation.result.get()
                        if (continuation.isActive) {
                            continuation.resume(Unit)
                        }
                    } catch (exception: ExecutionException) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                exception.cause ?: exception
                            )
                        }
                    } catch (exception: Exception) {
                        if (exception is InterruptedException) {
                            Thread.currentThread().interrupt()
                        }

                        if (continuation.isActive) {
                            continuation.resumeWithException(exception)
                        }
                    }
                },
                directExecutor
            )
        }
    }

    fun cancel(
        context: Context,
        downloadId: Long
    ): Operation {
        return WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(uniqueWorkName(downloadId))
    }

    fun cancelAll(context: Context): Operation {
        return WorkManager.getInstance(context.applicationContext)
            .cancelAllWorkByTag(WORK_TAG)
    }

    fun uniqueWorkName(downloadId: Long): String {
        return "$WORK_PREFIX$downloadId"
    }
}
