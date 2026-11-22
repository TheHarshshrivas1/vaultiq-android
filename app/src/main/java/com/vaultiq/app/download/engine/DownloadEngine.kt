
package com.vaultiq.app.download.engine

import android.net.Uri
import com.vaultiq.app.core.common.VaultResult
import com.vaultiq.app.core.utils.UrlValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long
) {
    val percentage: Int?
        get() = if (totalBytes > 0) {
            ((downloadedBytes * 100) / totalBytes)
                .toInt()
                .coerceIn(0, 100)
        } else {
            null
        }
}

data class DownloadSummary(
    val fileName: String,
    val mimeType: String?,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val destinationUri: Uri
)

class DownloadEngine(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    suspend fun download(
        url: String,
        destinationFactory: (
            String,
            String?
        ) -> VaultResult<DownloadOutput>,
        onProgress: (DownloadProgress) -> Unit = {}
    ): VaultResult<DownloadSummary> {

        val validation = UrlValidator.validate(url)

        if (validation !is VaultResult.Success) {
            return VaultResult.Error("Invalid download URL.")
        }

        return withContext(Dispatchers.IO) {

            val request = Request.Builder()
                .url(validation.data.toString())
                .get()
                .header("User-Agent", "VAULTIQ-Android/1.0")
                .build()

            val call = client.newCall(request)
            val job = currentCoroutineContext()[Job]

            val cancellationHandle = job?.invokeOnCompletion { cause ->
                if (cause != null) {
                    call.cancel()
                }
            }

            var createdOutput: DownloadOutput? = null
            var completed = false

            try {
                val response = call.execute()

                response.use {

                    if (!it.isSuccessful) {
                        return@withContext VaultResult.Error(
                            "Server returned HTTP ${it.code}."
                        )
                    }

                    val body = it.body

                    val mimeType = body.contentType()
                        ?.toString()
                        ?.substringBefore(";")
                        ?.lowercase()

                    if (
                        mimeType == "text/html" ||
                        mimeType == "application/json"
                    ) {
                        return@withContext VaultResult.Error(
                            "The URL did not return a downloadable media file."
                        )
                    }

                    val totalBytes = body.contentLength()

                    if (totalBytes == 0L) {
                        return@withContext VaultResult.Error(
                            "The server returned an empty file."
                        )
                    }

                    val fileName = resolveFileName(
                        it.header("Content-Disposition"),
                        validation.data.path
                    )

                    val destinationResult = destinationFactory(
                        fileName,
                        mimeType
                    )

                    val output = when (destinationResult) {
                        is VaultResult.Success -> destinationResult.data
                        is VaultResult.Error -> return@withContext destinationResult
                        is VaultResult.Loading -> {
                            return@withContext VaultResult.Error(
                                "Download destination is unavailable."
                            )
                        }
                    }

                    createdOutput = output

                    var downloadedBytes = 0L

                    output.outputStream.use { stream ->

                        val input = body.byteStream()
                        val buffer = ByteArray(64 * 1024)

                        while (true) {

                            currentCoroutineContext().ensureActive()

                            val bytesRead = input.read(buffer)

                            if (bytesRead == -1) {
                                break
                            }

                            stream.write(buffer, 0, bytesRead)

                            downloadedBytes += bytesRead

                            onProgress(
                                DownloadProgress(
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = totalBytes
                                )
                            )
                        }

                        stream.flush()
                    }

                    if (
                        totalBytes > 0 &&
                        downloadedBytes != totalBytes
                    ) {
                        return@withContext VaultResult.Error(
                            "Downloaded file size does not match the server response."
                        )
                    }

                    completed = true

                    VaultResult.Success(
                        DownloadSummary(
                            fileName = output.fileName,
                            mimeType = mimeType,
                            bytesDownloaded = downloadedBytes,
                            totalBytes = downloadedBytes,
                            destinationUri = output.uri
                        )
                    )
                }

            } catch (exception: CancellationException) {
                call.cancel()
                throw exception

            } catch (exception: Exception) {
                VaultResult.Error(
                    message = exception.message ?: "Download failed.",
                    cause = exception
                )

            } finally {
                cancellationHandle?.dispose()

                if (!completed && createdOutput != null) {
                    runCatching {
                        createdOutput!!.outputStream.close()
                    }

                    runCatching {
                        DownloadDestinationManagerCleanup.delete(
                            createdOutput!!.uri,
                            destinationFactory
                        )
                    }
                }
            }
        }
    }

    private fun resolveFileName(
        contentDisposition: String?,
        urlPath: String
    ): String {

        val headerName = contentDisposition?.let {
            Pattern.compile(
                """filename\*?=(?:UTF-8''|")?([^";]+)""",
                Pattern.CASE_INSENSITIVE
            ).matcher(it).let { matcher ->
                if (matcher.find()) matcher.group(1) else null
            }
        }

        val candidate = headerName
            ?: urlPath.substringAfterLast("/")
                .ifBlank { "download" }

        val safeName = candidate
            .substringAfterLast("/")
            .substringAfterLast("\\")
            .replace(
                Regex("""[\u0000-\u001F<>:"|?*]"""),
                "_"
            )
            .trim()
            .take(180)

        return safeName.ifBlank { "download" }
    }
}
