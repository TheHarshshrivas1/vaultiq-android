
package com.vaultiq.app.download.engine

import com.vaultiq.app.core.common.VaultResult
import com.vaultiq.app.core.utils.MediaSource
import com.vaultiq.app.core.utils.MediaSourceDetector
import com.vaultiq.app.core.utils.MediaType
import com.vaultiq.app.core.utils.MediaTypeDetection
import com.vaultiq.app.core.utils.MediaTypeDetector
import com.vaultiq.app.core.utils.SafeDns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.Locale
import java.util.concurrent.TimeUnit

data class DirectMediaInspection(
    val url: String,
    val httpCode: Int,
    val contentType: String?,
    val contentLength: Long?,
    val media: MediaTypeDetection,
    val fileName: String?
) {
    val isMediaFile: Boolean
        get() = media.type == MediaType.VIDEO ||
            media.type == MediaType.AUDIO ||
            media.type == MediaType.IMAGE
}

class DirectMediaInspector(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .dns(SafeDns)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {

    suspend fun inspect(
        url: String
    ): VaultResult<DirectMediaInspection> {

        val sourceResult = MediaSourceDetector.detect(url)

        val source = when (sourceResult) {
            is VaultResult.Success -> sourceResult.data.source

            is VaultResult.Error -> {
                return VaultResult.Error(sourceResult.message)
            }

            is VaultResult.Loading -> {
                return VaultResult.Error(
                    "Unable to validate the shared URL."
                )
            }
        }

        if (source != MediaSource.DIRECT_WEB) {
            return VaultResult.Error(
                "This is a ${source.displayName} link. " +
                    "Platform-specific downloading is not available."
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val headRequest = Request.Builder()
                    .url(url)
                    .head()
                    .header("User-Agent", "VAULTIQ-Android/1.0")
                    .build()

                val headResult:
                    VaultResult<DirectMediaInspection>? =
                    client.newCall(headRequest)
                        .execute()
                        .use { response ->

                            if (!response.isSuccessful) {
                                null
                            } else {
                                when (
                                    val parsed = parseResponse(
                                        url = response.request.url.toString(),
                                        response = response
                                    )
                                ) {
                                    is VaultResult.Success -> parsed
                                    is VaultResult.Error -> null
                                    is VaultResult.Loading -> null
                                }
                            }
                        }

                if (headResult is VaultResult.Success) {
                    return@withContext headResult
                }

                // Some servers reject HEAD or omit media headers.
                // Retry inspection using a one-byte range request.
                inspectWithRangeRequest(url)

            } catch (exception: CancellationException) {
                throw exception

            } catch (exception: Exception) {
                VaultResult.Error(
                    exception.message
                        ?: "Unable to inspect this media link.",
                    exception
                )
            }
        }
    }

    private fun inspectWithRangeRequest(
        url: String
    ): VaultResult<DirectMediaInspection> {

        val request = Request.Builder()
            .url(url)
            .get()
            .header("Range", "bytes=0-0")
            .header("User-Agent", "VAULTIQ-Android/1.0")
            .build()

        return try {
            client.newCall(request).execute().use { response ->

                if (!response.isSuccessful) {
                    return@use VaultResult.Error(
                        "Media server returned HTTP ${response.code}."
                    )
                }

                if (response.code != 206 && response.code != 200) {
                    return@use VaultResult.Error(
                        "Server did not return a supported media response."
                    )
                }

                val totalLength = if (response.code == 206) {
                    response.header("Content-Range")
                        ?.substringAfterLast("/")
                        ?.toLongOrNull()
                        ?.takeIf { it >= 0L }
                } else {
                    response.header("Content-Length")
                        ?.toLongOrNull()
                        ?.takeIf { it >= 0L }
                }

                parseResponse(
                    url = response.request.url.toString(),
                    response = response,
                    contentLengthOverride = totalLength
                )
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            VaultResult.Error(
                exception.message
                    ?: "Range inspection failed.",
                exception
            )
        }
    }

    private fun parseResponse(
        url: String,
        response: Response,
        contentLengthOverride: Long? = null
    ): VaultResult<DirectMediaInspection> {

        val contentType = response.header("Content-Type")
            ?.substringBefore(";")
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.takeIf { it.isNotBlank() }

        if (
            contentType == "text/html" ||
            contentType == "application/xhtml+xml" ||
            contentType == "application/json" ||
            contentType == "text/json" ||
            contentType == "application/xml" ||
            contentType == "text/xml" ||
            contentType?.startsWith("text/") == true
        ) {
            return VaultResult.Error(
                "The server returned a webpage or text response, not a media file."
            )
        }

        val contentLength = contentLengthOverride
            ?: response.header("Content-Length")
                ?.toLongOrNull()
                ?.takeIf { it >= 0L }

        val fileName = resolveFileName(
            response.header("Content-Disposition")
        )

        val detectionUrl = fileName
            ?.takeIf { it.isNotBlank() }
            ?: url

        val detectedMedia = MediaTypeDetector.detect(
            url = detectionUrl,
            contentType = contentType
        )

        val media = MediaTypeDetection(
            type = detectedMedia.type,
            mimeType = contentType,
            extension = detectedMedia.extension
        )

        val inspection = DirectMediaInspection(
            url = url,
            httpCode = response.code,
            contentType = contentType,
            contentLength = contentLength,
            media = media,
            fileName = fileName
        )

        return if (inspection.isMediaFile) {
            VaultResult.Success(inspection)
        } else {
            VaultResult.Error(
                "The server did not confirm a supported video, audio or image file."
            )
        }
    }

    private fun resolveFileName(
        contentDisposition: String?
    ): String? {

        if (contentDisposition.isNullOrBlank()) {
            return null
        }

        val extendedName = Regex(
            """filename\*\s*=\s*(?:UTF-8'')?([^;]+)""",
            RegexOption.IGNORE_CASE
        ).find(contentDisposition)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.trim('"', '\'')

        val regularName = Regex(
            """filename\s*=\s*"((?:\\.|[^"])*)"""",
            RegexOption.IGNORE_CASE
        ).find(contentDisposition)
            ?.groupValues
            ?.getOrNull(1)
            ?: Regex(
                """filename\s*=\s*([^;]+)""",
                RegexOption.IGNORE_CASE
            ).find(contentDisposition)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()
                ?.trim('"')

        return (extendedName ?: regularName)
            ?.substringAfterLast("/")
            ?.substringAfterLast("\\")
            ?.replace("\\\"", "\"")
            ?.takeIf { it.isNotBlank() }
    }
}
