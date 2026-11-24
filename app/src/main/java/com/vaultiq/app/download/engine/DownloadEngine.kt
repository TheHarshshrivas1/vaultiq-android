
package com.vaultiq.app.download.engine

import android.net.Uri
import com.vaultiq.app.core.common.VaultResult
import com.vaultiq.app.core.utils.MediaType
import com.vaultiq.app.core.utils.MediaTypeDetector
import com.vaultiq.app.core.utils.SafeDns
import com.vaultiq.app.core.utils.UrlValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

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
        .dns(SafeDns)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {

    suspend fun download(
        url: String,
        destinationFactory: (
            String,
            String?
        ) -> VaultResult<DownloadOutput>,
        cleanupDestination: (
            Uri
        ) -> VaultResult<Unit>,
        onProgress: (DownloadProgress) -> Unit = {}
    ): VaultResult<DownloadSummary> {

        val validation = UrlValidator.validate(url)

        if (validation !is VaultResult.Success) {
            return VaultResult.Error("Invalid download URL.")
        }

        return withContext(Dispatchers.IO) {

            val request = Request.Builder()
                .url(validation.data.toASCIIString())
                .get()
                .header("User-Agent", "VAULTIQ-Android/1.0")
                .build()

            val call = client.newCall(request)
            val job = currentCoroutineContext()[Job]

            val cancellationHandle = job?.invokeOnCompletion { cause ->
                if (cause != null) call.cancel()
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
                        ?: return@withContext VaultResult.Error(
                            "Server returned an empty response body."
                        )

                    val mimeType = body.contentType()
                        ?.toString()
                        ?.substringBefore(";")
                        ?.trim()
                        ?.lowercase()

                    val totalBytes = body.contentLength()

                    if (totalBytes == 0L) {
                        return@withContext VaultResult.Error(
                            "The server returned an empty file."
                        )
                    }

                    val finalUrlPath =
                        it.request.url.pathSegments.lastOrNull().orEmpty()

                    val fileName = resolveFileName(
                        it.header("Content-Disposition"),
                        finalUrlPath
                    )

                    val mediaCheck = validateMediaResponse(
                        fileName = fileName,
                        mimeType = mimeType
                    )

                    if (mediaCheck !is VaultResult.Success) {
                        return@withContext mediaCheck
                    }

                    val destinationResult = destinationFactory(
                        fileName,
                        mimeType
                    )

                    val output = when (destinationResult) {
                        is VaultResult.Success -> destinationResult.data

                        is VaultResult.Error -> {
                            return@withContext destinationResult
                        }

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

                            if (bytesRead == -1) break

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

                    if (downloadedBytes <= 0L) {
                        return@withContext VaultResult.Error(
                            "The downloaded file is empty."
                        )
                    }

                    completed = true

                    VaultResult.Success(
                        DownloadSummary(
                            fileName = output.fileName,
                            mimeType = mimeType,
                            bytesDownloaded = downloadedBytes,
                            totalBytes = if (totalBytes > 0) {
                                totalBytes
                            } else {
                                downloadedBytes
                            },
                            destinationUri = output.uri
                        )
                    )
                }

            } catch (exception: CancellationException) {
                call.cancel()
                throw exception

            } catch (exception: Exception) {
                VaultResult.Error(
                    message = exception.message
                        ?: "Download failed.",
                    cause = exception
                )

            } finally {
                cancellationHandle?.dispose()

                if (!completed) {
                    createdOutput?.let { output ->
                        runCatching {
                            output.outputStream.close()
                        }

                        runCatching {
                            cleanupDestination(output.uri)
                        }
                    }
                }
            }
        }
    }

    private fun validateMediaResponse(
        fileName: String,
        mimeType: String?
    ): VaultResult<Unit> {

        if (
            mimeType == "text/html" ||
            mimeType == "application/json" ||
            mimeType == "text/json" ||
            mimeType == "application/xml" ||
            mimeType == "text/xml"
        ) {
            return VaultResult.Error(
                "The server returned a webpage or data response, not a media file."
            )
        }

        val detected = MediaTypeDetector.detect(
            url = fileName,
            contentType = mimeType
        )

        if (
            detected.type == MediaType.UNKNOWN ||
            detected.type == MediaType.WEB_PAGE
        ) {
            return VaultResult.Error(
                "Unsupported or unrecognized file format."
            )
        }

        val genericMime = mimeType == null ||
            mimeType == "application/octet-stream" ||
            mimeType == "binary/octet-stream"

        if (!genericMime) {
            val mimeDetection = MediaTypeDetector.detect(
                url = "",
                contentType = mimeType
            )

            if (mimeDetection.type == MediaType.UNKNOWN) {
                return VaultResult.Error(
                    "The server returned an unsupported content type."
                )
            }
        }

        return VaultResult.Success(Unit)
    }

    private fun resolveFileName(
        contentDisposition: String?,
        urlPath: String
    ): String {

        val extendedName = contentDisposition?.let { header ->
            Regex(
                """filename\*\s*=\s*([^;]+)""",
                RegexOption.IGNORE_CASE
            ).find(header)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()
                ?.trim('"', '\'')
                ?.let { raw ->
                    val encoded = raw.substringAfter("''", raw)

                    runCatching {
                        URLDecoder.decode(
                            encoded.replace("+", "%2B"),
                            StandardCharsets.UTF_8.name()
                        )
                    }.getOrNull()
                }
        }

        val regularName = contentDisposition?.let { header ->
            val quoted = Regex(
                """filename\s*=\s*"((?:\\.|[^"])*)"""",
                RegexOption.IGNORE_CASE
            ).find(header)
                ?.groupValues
                ?.getOrNull(1)

            val unquoted = Regex(
                """filename\s*=\s*([^;]+)""",
                RegexOption.IGNORE_CASE
            ).find(header)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()
                ?.trim('"')

            (quoted ?: unquoted)?.replace("\\\"", "\"")
        }

        val candidate = extendedName
            ?.takeIf { it.isNotBlank() }
            ?: regularName?.takeIf { it.isNotBlank() }
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
