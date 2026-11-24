
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
        get() = media.type != MediaType.UNKNOWN &&
            media.type != MediaType.WEB_PAGE
}

class DirectMediaInspector(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .dns(SafeDns)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {

    suspend fun inspect(url: String): VaultResult<DirectMediaInspection> {
        val sourceResult = MediaSourceDetector.detect(url)

        val source = when (sourceResult) {
            is VaultResult.Success -> sourceResult.data.source
            is VaultResult.Error -> return VaultResult.Error(sourceResult.message)
            is VaultResult.Loading -> return VaultResult.Error(
                "Unable to validate the shared URL."
            )
        }

        if (source != MediaSource.DIRECT_WEB) {
            return VaultResult.Error(
                "This is a ${source.displayName} link. Platform-specific downloading is not available in this inspection step."
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val headRequest = Request.Builder()
                    .url(url)
                    .head()
                    .header("User-Agent", "VAULTIQ-Android/1.0")
                    .build()

                val headResult = client.newCall(headRequest)
                    .execute()
                    .use { response ->
                        when (response.code) {
                            405, 501 -> null
                            else -> {
                                if (!response.isSuccessful) {
                                    return@withContext VaultResult.Error(
                                        "Server returned HTTP ${response.code} after redirect processing."
                                    )
                                }

                                parseResponse(
                                    url = response.request.url.toString(),
                                    response = response
                                )
                            }
                        }
                    }

                if (headResult != null) {
                    return@withContext headResult
                }

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

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@use VaultResult.Error(
                    "Range inspection failed with HTTP ${response.code}."
                )
            }

            if (response.code != 206 && response.code != 200) {
                return@use VaultResult.Error(
                    "The server does not support the required range inspection."
                )
            }

            val totalLength = if (response.code == 206) {
                response.header("Content-Range")
                    ?.substringAfterLast("/")
                    ?.toLongOrNull()
                    ?.takeIf { it >= 0L }
            } else {
                null
            }

            parseResponse(
                url = response.request.url.toString(),
                response = response,
                contentLengthOverride = totalLength
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
            ?.takeIf { it.isNotBlank() }

        val contentLength = contentLengthOverride
            ?: response.header("Content-Length")
                ?.toLongOrNull()
                ?.takeIf { it >= 0L }

        val fileName = response.header("Content-Disposition")
            ?.let { header ->
                Regex(
                    """filename\*?=(?:UTF-8''|")?([^";]+)""",
                    RegexOption.IGNORE_CASE
                ).find(header)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.trim()
                    ?.substringAfterLast("/")
                    ?.substringAfterLast("\\")
            }

        val detectionUrl = fileName
            ?.takeIf { it.contains(".") }
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
                "The server did not confirm a supported media or document file."
            )
        }
    }
}
