
package com.vaultiq.app.download.engine

import com.vaultiq.app.core.common.VaultResult
import com.vaultiq.app.core.utils.MediaSource
import com.vaultiq.app.core.utils.MediaSourceDetector
import com.vaultiq.app.core.utils.MediaType
import com.vaultiq.app.core.utils.MediaTypeDetection
import com.vaultiq.app.core.utils.MediaTypeDetector
import com.vaultiq.app.core.utils.SafeDns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
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
        .callTimeout(15, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
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
                "This is a ${source.displayName} link, not a confirmed direct media file. Platform-specific support is not available in this inspection step."
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .head()
                    .header("User-Agent", "VAULTIQ-Android/1.0")
                    .build()

                client.newCall(request).execute().use { response ->

                    if (response.code in 300..399) {
                        return@withContext VaultResult.Error(
                            "The link redirects to another address. Redirect inspection is not supported yet."
                        )
                    }

                    if (!response.isSuccessful) {
                        return@withContext VaultResult.Error(
                            "Server returned HTTP ${response.code}."
                        )
                    }

                    val contentType = response.header("Content-Type")
                        ?.substringBefore(";")
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }

                    val contentLength = response.header("Content-Length")
                        ?.toLongOrNull()
                        ?.takeIf { it >= 0L }

                    val detectedMedia = MediaTypeDetector.detect(
                        url = url,
                        contentType = contentType
                    )

                    val media = MediaTypeDetection(
                        type = detectedMedia.type,
                        mimeType = contentType,
                        extension = detectedMedia.extension
                    )

                    val fileName = response.header("Content-Disposition")
                        ?.let { header ->
                            Regex(
                                """filename\*?=(?:UTF-8''|")?([^";]+)""",
                                RegexOption.IGNORE_CASE
                            )
                                .find(header)
                                ?.groupValues
                                ?.getOrNull(1)
                                ?.trim()
                        }

                    val inspection = DirectMediaInspection(
                        url = url,
                        httpCode = response.code,
                        contentType = contentType,
                        contentLength = contentLength,
                        media = media,
                        fileName = fileName
                    )

                    if (!inspection.isMediaFile) {
                        return@withContext VaultResult.Error(
                            "The server did not confirm a supported media or document file."
                        )
                    }

                    VaultResult.Success(inspection)
                }
            } catch (exception: Exception) {
                VaultResult.Error(
                    message = exception.message
                        ?: "Unable to inspect this media link.",
                    cause = exception
                )
            }
        }
    }
}
