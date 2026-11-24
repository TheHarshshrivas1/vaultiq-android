
package com.vaultiq.app.library

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.vaultiq.app.core.common.VaultResult
import com.vaultiq.app.download.engine.DownloadDestinationManager
import com.vaultiq.app.download.engine.DownloadOutput
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
            var destination: DownloadOutput? = null

            try {
                if (uri.scheme != "content") {
                    return@withContext VaultResult.Error(
                        "Unsupported file location."
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
                } ?: uri.lastPathSegment
                    ?.substringAfterLast("/")
                    ?: "imported_media"

                val providerMime = resolver.getType(uri)
                    ?.substringBefore(";")
                    ?.trim()
                    ?.lowercase()

                val extension = displayName
                    .substringAfterLast(".", "")
                    .lowercase()

                val extensionMime = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(extension)

                val mimeType = when {
                    providerMime?.startsWith("video/") == true ||
                    providerMime?.startsWith("audio/") == true ||
                    providerMime?.startsWith("image/") == true ->
                        providerMime

                    extensionMime?.startsWith("video/") == true ||
                    extensionMime?.startsWith("audio/") == true ||
                    extensionMime?.startsWith("image/") == true ->
                        extensionMime

                    else -> null
                }

                if (mimeType == null) {
                    return@withContext VaultResult.Error(
                        "Unsupported media file. Select a video, audio, or image."
                    )
                }

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
