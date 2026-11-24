
package com.vaultiq.app.core.utils

import java.net.URI
import java.util.Locale

enum class MediaType(
    val displayName: String
) {
    VIDEO("Video"),
    AUDIO("Audio"),
    IMAGE("Image"),
    DOCUMENT("Document"),
    WEB_PAGE("Web Page"),
    UNKNOWN("Unknown Format")
}

data class MediaTypeDetection(
    val type: MediaType,
    val mimeType: String?,
    val extension: String?
)

object MediaTypeDetector {

    fun detect(
        url: String,
        contentType: String? = null
    ): MediaTypeDetection {

        val mime = contentType
            ?.substringBefore(";")
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.takeIf { it.isNotBlank() }

        val extension = extractExtension(url)

        val detectedType = detectFromMime(mime)
            ?: detectFromExtension(extension)
            ?: MediaType.UNKNOWN

        return MediaTypeDetection(
            type = detectedType,
            mimeType = mime,
            extension = extension
        )
    }

    private fun detectFromMime(
        mime: String?
    ): MediaType? {

        if (mime == null) return null

        return when {
            mime.startsWith("video/") ->
                MediaType.VIDEO

            mime.startsWith("audio/") ->
                MediaType.AUDIO

            mime.startsWith("image/") ->
                MediaType.IMAGE

            mime == "text/html" ||
                mime == "application/xhtml+xml" ->
                MediaType.WEB_PAGE

            mime == "application/pdf" ||
                mime == "text/plain" ||
                mime == "text/csv" ||
                mime == "application/rtf" ||
                mime.contains("officedocument") ||
                mime == "application/msword" ||
                mime == "application/vnd.ms-excel" ||
                mime == "application/vnd.ms-powerpoint" ->
                MediaType.DOCUMENT

            mime == "application/octet-stream" ->
                null

            else -> null
        }
    }

    private fun detectFromExtension(
        extension: String?
    ): MediaType? {

        if (extension == null) return null

        return when (extension) {

            "mp4", "m4v", "mkv", "webm",
            "avi", "mov", "3gp", "mpeg", "mpg" ->
                MediaType.VIDEO

            "mp3", "m4a", "aac", "wav",
            "ogg", "opus", "flac", "wma" ->
                MediaType.AUDIO

            "jpg", "jpeg", "png", "webp",
            "gif", "bmp", "svg", "heic", "avif" ->
                MediaType.IMAGE

            "pdf", "txt", "csv", "rtf",
            "doc", "docx", "xls", "xlsx",
            "ppt", "pptx", "epub" ->
                MediaType.DOCUMENT

            "html", "htm" ->
                MediaType.WEB_PAGE

            else -> null
        }
    }

    private fun extractExtension(value: String): String? {
        if (value.isBlank()) return null

        val path = runCatching {
            URI(value).rawPath
        }.getOrNull()
            ?: value.substringBefore("?")
                .substringBefore("#")

        val fileName = path.substringAfterLast("/")
        val extension = fileName
            .substringAfterLast(".", "")
            .lowercase(Locale.ROOT)

        return extension.takeIf {
            it.isNotBlank() &&
                it.length <= 10 &&
                it.all { character ->
                    character.isLetterOrDigit()
                }
        }
    }
}
