
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
import java.util.Locale
import java.util.concurrent.TimeUnit

data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long
) {
    val percentage: Int?
        get() = if (totalBytes > 0L) {
            ((downloadedBytes * 100L) / totalBytes)
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

        val validUri = when (validation) {
            is VaultResult.Success -> validation.data

            is VaultResult.Error -> {
                return VaultResult.Error(
                    validation.message,
                    validation.cause
                )
            }

            is VaultResult.Loading -> {
                return VaultResult.Error(
                    "URL validation is still in progress."
                )
            }
        }

        return withContext(Dispatchers.IO) {

            val request = Request.Builder()
                .url(validUri.toASCIIString())
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

                response.use { response ->

                    if (!response.isSuccessful) {
                        return@use VaultResult.Error(
                            "Server returned HTTP ${response.code}."
                        )
                    }

                    val body = response.body
                        ?: return@use VaultResult.Error(
                            "Server returned an empty response body."
                        )

                    val mimeType = body.contentType()
                        ?.toString()
                        ?.substringBefore(";")
                        ?.trim()
                        ?.lowercase(Locale.ROOT)
                        ?.takeIf { it.isNotBlank() }

                    val totalBytes = body.contentLength()

                    if (totalBytes == 0L) {
                        return@use VaultResult.Error(
                            "The server returned an empty file."
                        )
                    }

                    val finalUrlPath =
                        response.request.url.encodedPath
                            .substringAfterLast("/")

                    val fileName = resolveFileName(
                        contentDisposition =
                            response.header("Content-Disposition"),
                        urlPath = finalUrlPath
                    )

                    val mediaCheck = validateMediaResponse(
                        fileName = fileName,
                        mimeType = mimeType
                    )

                    if (mediaCheck is VaultResult.Error) {
                        return@use VaultResult.Error(
                            mediaCheck.message,
                            mediaCheck.cause
                        )
                    }

                    if (mediaCheck !is VaultResult.Success) {
                        return@use VaultResult.Error(
                            "Unable to validate the media response."
                        )
                    }

                    val destinationResult = destinationFactory(
                        fileName,
                        mimeType
                    )

                    val output = when (destinationResult) {
                        is VaultResult.Success -> {
                            destinationResult.data
                        }

                        is VaultResult.Error -> {
                            return@use destinationResult
                        }

                        is VaultResult.Loading -> {
                            return@use VaultResult.Error(
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
                                        .takeIf { it > 0L }
                                        ?: 0L
                                )
                            )
                        }

                        stream.flush()
                    }

                    if (
                        totalBytes > 0L &&
                        downloadedBytes != totalBytes
                    ) {
                        return@use VaultResult.Error(
                            "Downloaded file size does not match the server response."
                        )
                    }

                    if (downloadedBytes <= 0L) {
                        return@use VaultResult.Error(
                            "The downloaded file is empty."
                        )
                    }

                    currentCoroutineContext().ensureActive()

                    completed = true

                    VaultResult.Success(
                        DownloadSummary(
                            fileName = output.fileName,
                            mimeType = mimeType,
                            bytesDownloaded = downloadedBytes,
                            totalBytes = if (totalBytes > 0L) {
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

        val normalizedMime = mimeType
            ?.substringBefore(";")
            ?.trim()
            ?.lowercase(Locale.ROOT)

        if (
            normalizedMime == "text/html" ||
            normalizedMime == "application/xhtml+xml" ||
            normalizedMime == "application/json" ||
            normalizedMime == "text/json" ||
            normalizedMime == "application/xml" ||
            normalizedMime == "text/xml" ||
            normalizedMime?.startsWith("text/") == true
        ) {
            return VaultResult.Error(
                "The server returned a webpage or text response, not a media file."
            )
        }

        val genericMime = normalizedMime == null ||
            normalizedMime == "application/octet-stream" ||
            normalizedMime == "binary/octet-stream"

        val detected = MediaTypeDetector.detect(
            url = fileName,
            contentType = normalizedMime
        )

        val supportedMedia = detected.type == MediaType.VIDEO ||
            detected.type == MediaType.AUDIO ||
            detected.type == MediaType.IMAGE

        if (!supportedMedia) {
            return VaultResult.Error(
                "Only video, audio and image files are supported."
            )
        }

        if (!genericMime) {
            val mimeDetection = MediaTypeDetector.detect(
                url = "",
                contentType = normalizedMime
            )

            val supportedMime =
                mimeDetection.type == MediaType.VIDEO ||
                    mimeDetection.type == MediaType.AUDIO ||
                    mimeDetection.type == MediaType.IMAGE

            if (!supportedMime) {
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

            (quoted ?: unquoted)
                ?.replace("\\\"", "\"")
        }

        val decodedUrlName = runCatching {
            URLDecoder.decode(
                urlPath.replace("+", "%2B"),
                StandardCharsets.UTF_8.name()
            )
        }.getOrNull()

        val candidate = extendedName
            ?.takeIf { it.isNotBlank() }
            ?: regularName?.takeIf { it.isNotBlank() }
            ?: decodedUrlName?.takeIf { it.isNotBlank() }
            ?: "download"

        val safeName = candidate
            .substringAfterLast("/")
            .substringAfterLast("\\")
            .replace(
                Regex("""[\u0000-\u001F<>:"|?*]"""),
                "_"
            )
            .trim()
            .take(180)

        return safeName.ifBlank {
            "download"
        }
    }
}
