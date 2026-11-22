
package com.vaultiq.app.download.queue

import android.content.Context
import android.net.Uri
import com.vaultiq.app.core.common.VaultResult
import com.vaultiq.app.core.utils.UrlValidator
import com.vaultiq.app.data.database.DownloadEntity
import com.vaultiq.app.data.database.VaultDatabase
import com.vaultiq.app.data.repository.DownloadRepository
import com.vaultiq.app.download.DownloadStatus

class DownloadRequestManager(context: Context) {

    private val appContext = context.applicationContext

    private val repository = DownloadRepository(
        VaultDatabase.getInstance(appContext).downloadDao()
    )

    suspend fun submit(rawUrl: String): VaultResult<Long> {

        val validation = UrlValidator.validate(rawUrl)

        if (validation !is VaultResult.Success) {
            return VaultResult.Error(
                (validation as? VaultResult.Error)?.message
                    ?: "Invalid download URL."
            )
        }

        val validatedUrl = validation.data.toASCIIString()

        val initialFileName = Uri.parse(validatedUrl)
            .lastPathSegment
            ?.takeIf { it.isNotBlank() }
            ?: "download"

        return try {

            val record = DownloadEntity(
                url = validatedUrl,
                fileName = initialFileName,
                status = DownloadStatus.QUEUED.name
            )

            val insertResult = repository.add(record)

            if (insertResult !is VaultResult.Success) {
                return VaultResult.Error(
                    (insertResult as? VaultResult.Error)?.message
                        ?: "Unable to create download record."
                )
            }

            val downloadId = insertResult.data

            when (
                val queueResult = DownloadQueue.enqueue(
                    context = appContext,
                    downloadId = downloadId,
                    url = validatedUrl
                )
            ) {
                is VaultResult.Success -> {
                    VaultResult.Success(downloadId)
                }

                is VaultResult.Error -> {

                    val cleanupResult =
                        repository.delete(downloadId)

                    if (cleanupResult !is VaultResult.Success) {
                        VaultResult.Error(
                            "Queue failed and the download record could not be removed.",
                            queueResult.cause
                        )
                    } else {
                        VaultResult.Error(
                            queueResult.message,
                            queueResult.cause
                        )
                    }
                }

                is VaultResult.Loading -> {
                    repository.delete(downloadId)

                    VaultResult.Error(
                        "Download could not be added to the queue."
                    )
                }
            }

        } catch (exception: Exception) {
            VaultResult.Error(
                "Unable to submit download request.",
                exception
            )
        }
    }
}
