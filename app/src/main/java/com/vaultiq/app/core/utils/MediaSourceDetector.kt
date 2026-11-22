
package com.vaultiq.app.core.utils

import com.vaultiq.app.core.common.VaultResult
import java.net.URI
import java.util.Locale

enum class MediaSource(
    val displayName: String
) {
    YOUTUBE("YouTube"),
    INSTAGRAM("Instagram"),
    FACEBOOK("Facebook"),
    TIKTOK("TikTok"),
    PINTEREST("Pinterest"),
    X("X"),
    DIRECT_WEB("Direct Web Link"),
    UNKNOWN("Unknown Source")
}

data class SourceDetection(
    val source: MediaSource,
    val host: String,
    val originalUrl: String
)

object MediaSourceDetector {

    fun detect(url: String): VaultResult<SourceDetection> {

        return when (val validation = UrlValidator.validate(url)) {

            is VaultResult.Success -> {
                val uri = validation.data
                val host = uri.host
                    ?.lowercase(Locale.ROOT)
                    ?.removePrefix("www.")
                    ?: return VaultResult.Error(
                        "Unable to identify URL host."
                    )

                val source = identifySource(host)

                VaultResult.Success(
                    SourceDetection(
                        source = source,
                        host = host,
                        originalUrl = uri.toString()
                    )
                )
            }

            is VaultResult.Error -> {
                VaultResult.Error(
                    validation.message,
                    validation.cause
                )
            }

            is VaultResult.Loading -> {
                VaultResult.Error(
                    "URL validation is still in progress."
                )
            }
        }
    }

    private fun identifySource(host: String): MediaSource {

        return when {
            host.matchesDomain("youtube.com") ||
                host.matchesDomain("youtu.be") ->
                MediaSource.YOUTUBE

            host.matchesDomain("instagram.com") ->
                MediaSource.INSTAGRAM

            host.matchesDomain("facebook.com") ||
                host.matchesDomain("fb.watch") ->
                MediaSource.FACEBOOK

            host.matchesDomain("tiktok.com") ->
                MediaSource.TIKTOK

            host.matchesDomain("pinterest.com") ||
                host.matchesDomain("pin.it") ->
                MediaSource.PINTEREST

            host.matchesDomain("x.com") ||
                host.matchesDomain("twitter.com") ->
                MediaSource.X

            else -> MediaSource.DIRECT_WEB
        }
    }

    private fun String.matchesDomain(domain: String): Boolean {
        return this == domain || this.endsWith(".$domain")
    }
}
