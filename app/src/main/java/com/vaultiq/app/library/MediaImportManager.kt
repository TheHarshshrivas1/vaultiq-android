
package com.vaultiq.app.library

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.vaultiq.app.core.common.VaultResult
import com.vaultiq.app.download.engine.DownloadDestinationManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

data class ImportedMedia(
    val uri: Uri,
    val fileName: String,
    val mimeType: String?,
    val sizeBytes: Long
)

class MediaImportManager(context: Context) {

    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val destinationManager =
        DownloadDestinationManager(appContext)

    suspend fun importMedia(uri: Uri): VaultResult<ImportedMedia> =
        withContext(Dispatchers.IO) {
            var destination: com.vaultiq.app.download.engine.DownloadOutput? = null

            try {
                if (uri.scheme != "content") {
                    return@withContext VaultResult.Error(
                        "Unsupported file location."
                    )
                }

                val mimeType = resolver.getType(uri)

                if (
                    mimeType == null ||
                    !(mimeType.startsWith("video/") ||
                      mimeType.startsWith("audio/") ||
                      mimeType.startsWith("image/"))
                ) {
                    return@withContext VaultResult.Error(
                        "Please select a video, audio, or image file."
                    )
                }

                val displayName = resolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(
                            OpenableColumns.DISPLAY_NAME
                        )
                        if (index >= 0) cursor.getString(index) else null
                    } else {
                        null
                    }
                } ?: uri.lastPathSegment?.substringAfterLast("/")
                    ?: "imported_media"

                val output = when (
                    val result = destinationManager.createDestination(
                        displayName,
                        mimeType
                    )
                ) {
                    is VaultResult.Success -> result.data
                    is VaultResult.Error -> return@withContext result
                    is VaultResult.Loading -> {
                        return@withContext VaultResult.Error(
                            "Storage is not ready."
                        )
                    }
                }

                destination = output

                var totalBytes = 0L

                resolver.openInputStream(uri)?.use { input ->
                    output.outputStream.use { stream ->
                        val buffer = ByteArray(64 * 1024)

                        while (true) {
                            val count = input.read(buffer)
                            if (count == -1) break

                            stream.write(buffer, 0, count)
                            totalBytes += count
                        }

                        stream.flush()
                    }
                } ?: throw IllegalStateException(
                    "Unable to open selected file."
                )

                if (totalBytes <= 0L) {
                    throw IllegalStateException(
                        "Selected file is empty."
                    )
                }

                VaultResult.Success(
                    ImportedMedia(
                        uri = output.uri,
                        fileName = output.fileName,
                        mimeType = mimeType,
                        sizeBytes = totalBytes
                    )
                )

            } catch (exception: CancellationException) {
                withContext(NonCancellable) {
                    destination?.let {
                        runCatching {
                            it.outputStream.close()
                            destinationManager.deleteDestination(it.uri)
                        }
                    }
                }
                throw exception

            } catch (exception: Exception) {
                destination?.let {
                    runCatching {
                        it.outputStream.close()
                        destinationManager.deleteDestination(it.uri)
                    }
                }

                VaultResult.Error(
                    exception.message ?: "Media import failed.",
                    exception
                )
            }
        }
}
